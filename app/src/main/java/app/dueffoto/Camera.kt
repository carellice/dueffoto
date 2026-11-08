package app.dueffoto

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * La fotocamera. Il mirino non è l'anteprima di sistema: sono fotogrammi sviluppati dal vivo con la
 * stessa pellicola dello scatto, così quello che vedi è quello che stampi.
 */
class CameraRig(private val context: Context) {
    private val analysisThread = Executors.newSingleThreadExecutor()
    private val captureThread = Executors.newSingleThreadExecutor()
    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var rotation = Surface.ROTATION_0

    var front = false
        private set
    var video = false
        private set

    /** Riceve ogni fotogramma del mirino, già dritto (e specchiato per la frontale). */
    var onFrame: ((Bitmap) -> Unit)? = null

    private val orientation = object : OrientationEventListener(context) {
        override fun onOrientationChanged(deg: Int) {
            if (deg == ORIENTATION_UNKNOWN) return
            rotation = when (deg) {
                in 45 until 135 -> Surface.ROTATION_270
                in 135 until 225 -> Surface.ROTATION_180
                in 225 until 315 -> Surface.ROTATION_90
                else -> Surface.ROTATION_0
            }
            imageCapture?.targetRotation = rotation
            videoCapture?.targetRotation = rotation
        }
    }

    suspend fun start(owner: LifecycleOwner, front: Boolean, video: Boolean) {
        this.front = front
        this.video = video
        val p = provider ?: suspendCancellableCoroutine { cont ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({ cont.resume(future.get()) }, ContextCompat.getMainExecutor(context))
        }.also { provider = it }
        p.unbindAll()

        val ratio = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
        val analysis = ImageAnalysis.Builder()
            .setResolutionSelector(
                ratio.setResolutionStrategy(
                    ResolutionStrategy(android.util.Size(720, 960), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER),
                ).build(),
            )
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analysis.setAnalyzer(analysisThread) { image -> image.use { onFrame?.invoke(upright(it, mirror = this.front)) } }

        val selector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        imageCapture = null
        videoCapture = null
        camera = if (video) {
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.fromOrderedList(listOf(Quality.FHD, Quality.HD, Quality.SD)))
                .build()
            val vc = VideoCapture.withOutput(recorder).also { it.targetRotation = rotation }
            videoCapture = vc
            p.bindToLifecycle(owner, selector, analysis, vc)
        } else {
            val ic = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy(AspectRatio.RATIO_4_3, AspectRatioStrategy.FALLBACK_RULE_AUTO))
                        .build(),
                )
                .setTargetRotation(rotation)
                .build()
            imageCapture = ic
            p.bindToLifecycle(owner, selector, analysis, ic)
        }
        orientation.enable()
    }

    fun stop() {
        orientation.disable()
        recording?.stop()
        provider?.unbindAll()
    }

    val hasFlash get() = camera?.cameraInfo?.hasFlashUnit() == true

    fun setFlash(on: Boolean) {
        imageCapture?.flashMode = if (on) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        if (video) camera?.cameraControl?.enableTorch(on)
    }

    /** Compensazione dell'esposizione in stop (−2..+2), arrotondata ai passi della fotocamera. */
    fun setExposure(ev: Float) {
        val state = camera?.cameraInfo?.exposureState ?: return
        if (!state.isExposureCompensationSupported) return
        val step = state.exposureCompensationStep.toFloat().takeIf { it > 0f } ?: return
        val index = Math.round(ev / step).coerceIn(state.exposureCompensationRange.lower, state.exposureCompensationRange.upper)
        camera?.cameraControl?.setExposureCompensationIndex(index)
    }

    fun zoomBy(factor: Float) {
        val state = camera?.cameraInfo?.zoomState?.value ?: return
        camera?.cameraControl?.setZoomRatio((state.zoomRatio * factor).coerceIn(state.minZoomRatio, state.maxZoomRatio))
    }

    /** Messa a fuoco nel punto toccato, in coordinate 0..1 del mirino (già dritto). */
    fun focus(u: Float, v: Float) {
        val cam = camera ?: return
        val deg = cam.cameraInfo.sensorRotationDegrees
        val x = if (front) 1f - u else u
        // Dal mirino dritto al sistema del sensore.
        val (sx, sy) = when (deg) {
            90 -> v to 1f - x
            180 -> 1f - x to 1f - v
            270 -> 1f - v to x
            else -> x to v
        }
        val point = SurfaceOrientedMeteringPointFactory(1f, 1f).createPoint(sx, sy)
        cam.cameraControl.startFocusAndMetering(FocusMeteringAction.Builder(point).build())
    }

    /** Scatta: la foto torna dritta, ridotta al massimo lato che ha senso sviluppare. */
    suspend fun shoot(): Bitmap {
        val ic = imageCapture ?: error("Fotocamera non pronta")
        return suspendCancellableCoroutine { cont ->
            ic.takePicture(captureThread, object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val result = runCatching { image.use { Darkroom.fit(upright(it, mirror = front), Darkroom.MAX_SIDE) } }
                    result.onSuccess { cont.resume(it) }.onFailure { cont.resumeWithException(it) }
                }

                override fun onError(exception: ImageCaptureException) = cont.resumeWithException(exception)
            })
        }
    }

    val recordingNow get() = recording != null

    /** Avvia la registrazione in un file temporaneo; [onDone] riceve il file a registrazione chiusa. */
    @SuppressLint("MissingPermission")
    fun record(onTick: (Long) -> Unit, onDone: (File?) -> Unit) {
        val vc = videoCapture ?: return
        val file = File(context.cacheDir, "rec-${System.currentTimeMillis()}.mp4")
        var pending = vc.output.prepareRecording(context, FileOutputOptions.Builder(file).build())
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            pending = pending.withAudioEnabled()
        }
        recording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Status -> onTick(event.recordingStats.recordedDurationNanos / 1_000_000)
                is VideoRecordEvent.Finalize -> {
                    recording = null
                    val ok = !event.hasError() || event.error == VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE
                    onDone(if (ok && file.length() > 0) file else file.delete().let { null })
                }
            }
        }
    }

    fun stopRecording() {
        recording?.stop()
    }

    fun release() {
        stop()
        analysisThread.shutdown()
        captureThread.shutdown()
    }

    private fun upright(image: ImageProxy, mirror: Boolean): Bitmap {
        val bmp = image.toBitmap()
        val deg = image.imageInfo.rotationDegrees
        if (deg == 0 && !mirror) return bmp
        val m = Matrix().apply {
            postRotate(deg.toFloat())
            if (mirror) postScale(-1f, 1f)
        }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true).also { if (it !== bmp) bmp.recycle() }
    }
}
