package app.dueffoto

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.max

/** Uno scatto nel rullino: l'originale "nudo", da sviluppare (anche di nuovo) con qualsiasi pellicola. */
data class Shot(
    val id: Long,
    val video: Boolean,
    val filmId: String?,
    val ratio: Ratio = Ratio.OPEN,
    val strength: Float = 1f,
) {
    val film get() = Films.byId(filmId)
}

/**
 * Il rullino: gli originali restano nella memoria dell'app, le stampe vanno nella galleria del telefono.
 * Così si può sempre cambiare idea sulla pellicola.
 */
class Roll(private val context: Context) {
    private val dir = File(context.filesDir, "roll").apply { mkdirs() }
    private val index = File(dir, "roll.json")
    private val _shots = MutableStateFlow(load())
    val shots: StateFlow<List<Shot>> = _shots

    fun file(shot: Shot) = File(dir, "${shot.id}.${if (shot.video) "mp4" else "jpg"}")

    /** Conserva una foto già dritta come originale. */
    suspend fun addPhoto(bitmap: Bitmap, film: Film?, ratio: Ratio, strength: Float): Shot = withContext(Dispatchers.IO) {
        val shot = Shot(System.currentTimeMillis(), false, film?.id, ratio, strength)
        file(shot).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        update { listOf(shot) + it }
        shot
    }

    /** Sposta nel rullino un video appena registrato. */
    suspend fun addVideo(raw: File, film: Film?, ratio: Ratio, strength: Float): Shot = withContext(Dispatchers.IO) {
        val shot = Shot(System.currentTimeMillis(), true, film?.id, ratio, strength)
        if (!raw.renameTo(file(shot))) {
            raw.copyTo(file(shot), overwrite = true)
            raw.delete()
        }
        update { listOf(shot) + it }
        shot
    }

    fun save(shot: Shot) = update { list -> list.map { if (it.id == shot.id) shot else it } }

    fun delete(shot: Shot) {
        file(shot).delete()
        update { list -> list.filterNot { it.id == shot.id } }
    }

    /** L'originale ridotto a [maxSide] (per anteprime e miniature). */
    suspend fun original(shot: Shot, maxSide: Int): Bitmap? = withContext(Dispatchers.IO) {
        val f = file(shot)
        if (!f.exists()) return@withContext null
        if (shot.video) {
            val r = MediaMetadataRetriever()
            try {
                r.setDataSource(f.absolutePath)
                r.getFrameAtTime(0)?.let { Darkroom.fit(it, maxSide) }
            } catch (_: Exception) {
                null
            } finally {
                r.release()
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.absolutePath, bounds)
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
            BitmapFactory.decodeFile(f.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
                ?.let { Darkroom.fit(it, maxSide) }
        }
    }

    private fun update(change: (List<Shot>) -> List<Shot>) {
        synchronized(this) {
            val next = change(_shots.value)
            _shots.value = next
            val json = JSONArray()
            next.forEach {
                json.put(
                    JSONObject().put("id", it.id).put("video", it.video).put("film", it.filmId ?: JSONObject.NULL)
                        .put("ratio", it.ratio.name).put("strength", it.strength.toDouble()),
                )
            }
            index.writeText(json.toString())
        }
    }

    private fun load(): List<Shot> = runCatching {
        val json = JSONArray(index.readText())
        (0 until json.length()).map { i ->
            val o = json.getJSONObject(i)
            Shot(
                o.getLong("id"), o.getBoolean("video"), o.optString("film").takeIf { !o.isNull("film") && it.isNotEmpty() },
                runCatching { Ratio.valueOf(o.optString("ratio")) }.getOrDefault(Ratio.OPEN),
                o.optDouble("strength", 1.0).toFloat(),
            )
        }.filter { File(dir, "${it.id}.${if (it.video) "mp4" else "jpg"}").exists() }
    }.getOrDefault(emptyList())
}

/** Salvataggio delle stampe nella galleria, nella cartella Dueffoto. */
object Gallery {
    suspend fun savePhoto(context: Context, bitmap: Bitmap, name: String): Uri? = withContext(Dispatchers.IO) {
        write(context, "$name.jpg", "image/jpeg", Environment.DIRECTORY_PICTURES) { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 94, out)
        }
    }

    suspend fun saveVideo(context: Context, file: File, name: String): Uri? = withContext(Dispatchers.IO) {
        write(context, "$name.mp4", "video/mp4", Environment.DIRECTORY_MOVIES) { out ->
            file.inputStream().use { it.copyTo(out) }
        }
    }

    private fun write(context: Context, name: String, mime: String, base: String, body: (java.io.OutputStream) -> Unit): Uri? {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = if (mime.startsWith("video")) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$base/Dueffoto")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values) ?: return null
            try {
                resolver.openOutputStream(uri)?.use(body) ?: error("Impossibile scrivere")
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                return uri
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        }
        @Suppress("DEPRECATION")
        val folder = File(Environment.getExternalStoragePublicDirectory(base), "Dueffoto").apply { mkdirs() }
        val file = File(folder, name)
        file.outputStream().use(body)
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mime), null)
        return Uri.fromFile(file)
    }
}
