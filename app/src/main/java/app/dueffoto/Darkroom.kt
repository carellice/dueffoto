package app.dueffoto

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.stream.IntStream
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Opzioni dello sviluppo che non dipendono dalla pellicola. */
data class Develop(
    val strength: Float = 1f,
    val ratio: Ratio = Ratio.OPEN,
    val seed: Int = 7,
    val grain: Boolean = true,
    val frame: Boolean = true,
    /** Numero del fotogramma sul bordo pellicola (0 = ricavato da [seed]). */
    val number: Int = 0,
)

/** La camera oscura: dalla foto "nuda" alla stampa con colore, grana, vignettatura e cornice. */
object Darkroom {
    /** Carattere della data e del bordo pellicola (impostato all'avvio). */
    var mono: Typeface = Typeface.MONOSPACE

    /** Rapporto (lato lungo / corto) a cui ritagliare, o null. */
    fun aspectOf(film: Film, opts: Develop): Float? = when (film.frame) {
        Frame.INSTANT, Frame.SQUARE -> 1f
        Frame.INSTAX -> 62f / 46f
        else -> film.aspect ?: if (film.kind == Kind.CINEMA) opts.ratio.value else null
    }

    fun develop(src: Bitmap, film: Film?, opts: Develop = Develop()): Bitmap {
        if (film == null) return src.copy(Bitmap.Config.ARGB_8888, true)
        val cropped = crop(src, aspectOf(film, opts))
        val w = cropped.width
        val h = cropped.height
        val px = IntArray(w * h)
        cropped.getPixels(px, 0, w, 0, 0, w, h)
        if (cropped !== src) cropped.recycle()

        val minDim = min(w, h).toFloat()
        val cx = w / 2f
        val cy = h / 2f
        // Grana proporzionata alla stampa: su 12 MP i granelli sono di qualche pixel, non di uno.
        val cell = max(1f, minDim / 900f) * (0.85f + film.grain * 0.5f)
        val grainAmp = if (opts.grain) film.grain * 0.11f * opts.strength else 0f
        val vig = film.vignette * opts.strength
        val leak = film.leak * opts.strength
        val leakX = w * 1.02f
        val leakY = h * 0.18f
        val leakR = minDim * 0.55f

        val rows = 32
        IntStream.range(0, rows).parallel().forEach { chunk ->
            val grade = Grade(film, opts.strength)
            val c = FloatArray(3)
            val y0 = h * chunk / rows
            val y1 = h * (chunk + 1) / rows
            for (y in y0 until y1) {
                val dy = (y - cy) / cy
                for (x in 0 until w) {
                    val i = y * w + x
                    val p = px[i]
                    grade.apply(
                        ((p shr 16) and 0xFF) / 255f, ((p shr 8) and 0xFF) / 255f, (p and 0xFF) / 255f, c,
                    )
                    var r = c[0]
                    var g = c[1]
                    var b = c[2]
                    if (vig > 0f) {
                        val dx = (x - cx) / cx
                        val d = dx * dx + dy * dy
                        val v = 1f - vig * 0.6f * smooth(0.25f, 2f, d)
                        r *= v; g *= v; b *= v
                    }
                    if (leak > 0f) {
                        val lx = (x - leakX) / leakR
                        val ly = (y - leakY) / (leakR * 1.6f)
                        val lw = leak * exp(-(lx * lx + ly * ly))
                        r = 1f - (1f - r) * (1f - lw * 1.0f)
                        g = 1f - (1f - g) * (1f - lw * 0.42f)
                        b = 1f - (1f - b) * (1f - lw * 0.12f)
                    }
                    if (grainAmp > 0f) {
                        val l = (0.2126f * r + 0.7152f * g + 0.0722f * b).coerceIn(0f, 1f)
                        // Più grana nei mezzitoni, quasi niente nei neri e nei bianchi pieni.
                        val m = 1f - (2f * l - 1f).let { it * it }
                        val n = (valueNoise(x / cell, y / cell, opts.seed) * 0.75f +
                            hash(x, y, opts.seed + 31) * 0.25f) * grainAmp * (0.35f + 0.65f * m)
                        r += n; g += n; b += n
                    }
                    px[i] = (0xFF shl 24) or (to8(r) shl 16) or (to8(g) shl 8) or to8(b)
                }
            }
        }
        val graded = Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
        return if (opts.frame) print(graded, film, if (opts.number > 0) opts.number else opts.seed % 36 + 1) else graded
    }

    /** Ritaglio centrale al rapporto [ratio] (lato lungo/corto), mantenendo l'orientamento della foto. */
    fun crop(src: Bitmap, ratio: Float?): Bitmap {
        if (ratio == null) return src
        val w = src.width
        val h = src.height
        val target = if (w >= h) ratio else 1f / ratio
        val current = w.toFloat() / h
        if (kotlin.math.abs(current - target) < 0.01f) return src
        return if (current > target) {
            val nw = (h * target).roundToInt()
            Bitmap.createBitmap(src, (w - nw) / 2, 0, nw, h)
        } else {
            val nh = (w / target).roundToInt()
            Bitmap.createBitmap(src, 0, (h - nh) / 2, w, nh)
        }
    }

    /** Aggiunge cornice e data: la stampa finita. */
    private fun print(img: Bitmap, film: Film, number: Int): Bitmap {
        val dated = if (film.date) stampDate(img) else img
        return when (film.frame) {
            Frame.NONE -> dated
            Frame.INSTANT -> card(dated, side = 0.06f, top = 0.06f, bottom = 0.3f, paper = 0xFFF3F0E8.toInt())
            Frame.INSTAX -> card(dated, side = 4f / 46f, top = 7f / 46f, bottom = 17f / 46f, paper = 0xFFFAFAF7.toInt())
            Frame.SQUARE -> card(dated, side = 0.05f, top = 0.05f, bottom = 0.05f, paper = 0xFFEDE6D6.toInt(), corner = 0.03f)
            Frame.REBATE -> rebate(dated, film, number)
        }
    }

    private fun card(img: Bitmap, side: Float, top: Float, bottom: Float, paper: Int, corner: Float = 0f): Bitmap {
        val w = img.width
        val s = (w * side).roundToInt()
        val t = (w * top).roundToInt()
        val b = (w * bottom).roundToInt()
        val out = Bitmap.createBitmap(w + 2 * s, img.height + t + b, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(if (corner > 0f) 0xFF14110E.toInt() else paper)
        if (corner > 0f) {
            paint.color = paper
            val r = w * corner
            canvas.drawRoundRect(RectF(0f, 0f, out.width.toFloat(), out.height.toFloat()), r, r, paint)
        }
        canvas.drawBitmap(img, s.toFloat(), t.toFloat(), null)
        // Il bordo della foto leggermente infossato nella carta.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(1f, w / 600f)
        paint.color = 0x22000000
        canvas.drawRect(s.toFloat(), t.toFloat(), (s + w).toFloat(), (t + img.height).toFloat(), paint)
        img.recycle()
        return out
    }

    /** Il fotogramma con il bordo della pellicola: nero, perforazioni e scritte arancioni. */
    private fun rebate(img: Bitmap, film: Film, number: Int): Bitmap {
        val portrait = img.height > img.width
        val short = min(img.width, img.height)
        val band = (short * 0.16f).roundToInt()
        val pad = (short * 0.02f).roundToInt()
        val ow = if (portrait) img.width + 2 * band else img.width + 2 * pad
        val oh = if (portrait) img.height + 2 * pad else img.height + 2 * band
        val out = Bitmap.createBitmap(ow, oh, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(0xFF0D0B09.toInt())
        val ix = if (portrait) band.toFloat() else pad.toFloat()
        val iy = if (portrait) pad.toFloat() else band.toFloat()
        canvas.drawBitmap(img, ix, iy, null)
        img.recycle()

        // Lavoriamo come se la striscia fosse orizzontale e ruotiamo la tela se la foto è verticale.
        canvas.save()
        if (portrait) {
            canvas.rotate(-90f)
            canvas.translate(-oh.toFloat(), 0f)
        }
        val stripW = if (portrait) oh.toFloat() else ow.toFloat()
        val stripH = if (portrait) ow.toFloat() else oh.toFloat()
        val hole = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE9E1CF.toInt() }
        val hw = band * 0.26f
        val hh = band * 0.36f
        val step = hw * 2.3f
        var x = step * 0.4f
        while (x + hw < stripW) {
            for (yy in listOf(band * 0.12f, stripH - band * 0.12f - hh)) {
                canvas.drawRoundRect(RectF(x, yy, x + hw, yy + hh), hw * 0.22f, hw * 0.22f, hole)
            }
            x += step
        }
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF08A2C.toInt()
            typeface = mono
            textSize = band * 0.24f
            letterSpacing = 0.12f
        }
        val name = film.stock.uppercase().replace("KODAK ", "").replace("FUJI ", "")
        canvas.drawText("DUEFFOTO  $name  ${film.iso}", stripW * 0.06f, stripH - band * 0.6f, text)
        canvas.drawText("▸ ${number}A", stripW * 0.72f, stripH - band * 0.6f, text)
        canvas.drawText("${film.iso} ▸▸", stripW * 0.3f, band * 0.82f, text)
        canvas.restore()
        return out
    }

    /** La data arancione delle compatte anni '90, stampata in basso a destra. */
    private fun stampDate(img: Bitmap): Bitmap {
        val out = if (img.isMutable) img else img.copy(Bitmap.Config.ARGB_8888, true).also { img.recycle() }
        val canvas = Canvas(out)
        val short = min(out.width, out.height)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFF8A1E.toInt()
            typeface = mono
            textSize = short * 0.045f
            letterSpacing = 0.08f
            setShadowLayer(short * 0.012f, 0f, 0f, 0xCCFF5A00.toInt())
        }
        val text = LocalDate.now().format(DateTimeFormatter.ofPattern("''yy  M  d"))
        val tw = paint.measureText(text)
        canvas.drawText(text, out.width - tw - short * 0.06f, out.height - short * 0.06f, paint)
        return out
    }

    /** La pellicola come LUT 3D (cubo [R][G][B] di colori ARGB), per i video. */
    fun lut(film: Film, strength: Float, size: Int = 33): Array<Array<IntArray>> {
        val grade = Grade(film, strength)
        val c = FloatArray(3)
        val n = (size - 1).toFloat()
        return Array(size) { r ->
            Array(size) { g ->
                IntArray(size) { b ->
                    grade.apply(r / n, g / n, b / n, c)
                    Color.argb(255, to8(c[0]), to8(c[1]), to8(c[2]))
                }
            }
        }
    }

    private fun to8(v: Float): Int = (v * 255f + 0.5f).toInt().coerceIn(0, 255)

    private fun smooth(e0: Float, e1: Float, x: Float): Float {
        val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** Rumore −1..1 deterministico per pixel. */
    private fun hash(x: Int, y: Int, seed: Int): Float {
        var h = x * 374761393 + y * 668265263 + seed * 144269504
        h = (h xor (h ushr 13)) * 1274126177
        h = h xor (h ushr 16)
        return (h and 0xFFFF) / 32767.5f - 1f
    }

    /** Rumore morbido: i granelli hanno una dimensione, non sono pixel isolati. */
    private fun valueNoise(fx: Float, fy: Float, seed: Int): Float {
        val x0 = fx.toInt()
        val y0 = fy.toInt()
        val tx = fx - x0
        val ty = fy - y0
        val a = hash(x0, y0, seed)
        val b = hash(x0 + 1, y0, seed)
        val c = hash(x0, y0 + 1, seed)
        val d = hash(x0 + 1, y0 + 1, seed)
        val top = a + (b - a) * tx
        val bottom = c + (d - c) * tx
        return (top + (bottom - top) * ty) * 1.4f
    }

    /** Lato lungo massimo dello sviluppo finale: oltre non si vede differenza e si consuma memoria. */
    const val MAX_SIDE = 4096

    fun fit(src: Bitmap, maxSide: Int): Bitmap {
        val long = max(src.width, src.height)
        if (long <= maxSide) return src
        val k = maxSide.toFloat() / long
        return Bitmap.createScaledBitmap(src, (src.width * k).roundToInt(), (src.height * k).roundToInt(), true)
    }
}
