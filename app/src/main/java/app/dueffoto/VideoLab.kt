package app.dueffoto

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.effect.Presentation
import androidx.media3.effect.SingleColorLut
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Sviluppo dei video: la pellicola diventa una LUT 3D applicata a ogni fotogramma, più il ritaglio cinema.
 * Grana, vignettatura e cornici restano alle foto.
 */
object VideoLab {
    /** Sviluppa [raw] in un file temporaneo, riportando l'avanzamento 0..1. Va chiamato dal thread principale. */
    suspend fun develop(context: Context, raw: File, film: Film, opts: Develop, onProgress: (Float) -> Unit): File {
        val out = File(context.cacheDir, "lab-${System.currentTimeMillis()}.mp4")
        val effects = buildList {
            add(SingleColorLut.createFromCube(Darkroom.lut(film, opts.strength)))
            Darkroom.aspectOf(film, opts)?.let { ratio ->
                val portrait = isPortrait(raw)
                add(Presentation.createForAspectRatio(if (portrait) 1f / ratio else ratio, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
            }
        }
        val item = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(raw)))
            .setEffects(Effects(emptyList(), effects))
            .build()
        val handler = Handler(Looper.getMainLooper())
        return suspendCancellableCoroutine { cont ->
            lateinit var transformer: Transformer
            val holder = ProgressHolder()
            val poll = object : Runnable {
                override fun run() {
                    if (transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) onProgress(holder.progress / 100f)
                    handler.postDelayed(this, 250)
                }
            }
            transformer = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        handler.removeCallbacks(poll)
                        onProgress(1f)
                        cont.resume(out)
                    }

                    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                        handler.removeCallbacks(poll)
                        out.delete()
                        cont.resumeWithException(exportException)
                    }
                })
                .build()
            transformer.start(item, out.absolutePath)
            handler.post(poll)
            cont.invokeOnCancellation {
                handler.post {
                    handler.removeCallbacks(poll)
                    transformer.cancel()
                    out.delete()
                }
            }
        }
    }

    /** Il video si guarda in verticale (contando la rotazione salvata nei metadati)? */
    private fun isPortrait(file: File): Boolean {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(file.absolutePath)
            val w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rot = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (rot == 90 || rot == 270) w > h else h > w
        } finally {
            r.release()
        }
    }
}
