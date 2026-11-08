package app.dueffoto

import kotlin.math.pow

/** Analogico: emula una fotocamera con la sua pellicola. Cinema: un look da color grading moderno. */
enum class Kind { ANALOG, CINEMA }

/** Come esce la stampa: senza bordo, istantanea, formato quadrato o con il bordo della pellicola. */
enum class Frame { NONE, INSTANT, INSTAX, SQUARE, REBATE }

/** Uno spostamento di colore (−0.1..0.1 è già tanto) aggiunto alle ombre o alle luci. */
data class Tint(val r: Float = 0f, val g: Float = 0f, val b: Float = 0f)

/**
 * Una pellicola: tutte le regole con cui lo scatto viene "sviluppato".
 * Valori pensati per variare poco: un look riconoscibile ma credibile.
 */
data class Film(
    val id: String,
    val kind: Kind,
    /** Fotocamera (analogico) o nome del look (cinema). */
    val camera: String,
    /** Pellicola (analogico) o riferimento della pellicola da cinema. */
    val stock: String,
    val year: String,
    val note: String,
    /** Colori del rullino disegnato nella scelta: corpo, fascia, scritta. */
    val canister: Triple<Long, Long, Long>,
    val iso: Int = 400,
    val exposure: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 1f,
    val warmth: Float = 0f,
    val tint: Float = 0f,
    val shadows: Tint = Tint(),
    val highlights: Tint = Tint(),
    /** Neri sollevati (pellicola slavata). */
    val fade: Float = 0f,
    /** Bianchi abbassati (stampa opaca). */
    val roll: Float = 0f,
    val bw: Boolean = false,
    val grain: Float = 0f,
    val vignette: Float = 0f,
    val leak: Float = 0f,
    val frame: Frame = Frame.NONE,
    val date: Boolean = false,
    /** Rapporto del lato lungo sul corto, null = come la fotocamera. */
    val aspect: Float? = null,
)

/** Le bobine disponibili, nell'ordine in cui compaiono. */
object Films {
    val analog = listOf(
        Film(
            "trip35", Kind.ANALOG, "Olympus Trip 35", "Kodak Gold 200", "1967",
            "Gialli caldi e pelle dorata: le vacanze di famiglia.",
            Triple(0xFFF2B705, 0xFFD3271C, 0xFF2A1A0A), iso = 200,
            contrast = 0.15f, saturation = 1.08f, warmth = 0.55f, tint = 0.05f,
            shadows = Tint(0.02f, 0.01f, -0.03f), highlights = Tint(0.04f, 0.02f, -0.04f),
            fade = 0.04f, roll = 0.03f, grain = 0.35f, vignette = 0.25f,
        ),
        Film(
            "ae1", Kind.ANALOG, "Canon AE-1", "Kodak Portra 400", "1976",
            "Pastelli morbidi, ombre aperte, la pellicola dei ritratti.",
            Triple(0xFFF5F0E6, 0xFFE07B1F, 0xFF1C2F5A),
            contrast = -0.1f, saturation = 0.88f, warmth = 0.3f,
            shadows = Tint(0.0f, 0.015f, 0.025f), highlights = Tint(0.03f, 0.015f, -0.02f),
            fade = 0.06f, roll = 0.04f, grain = 0.3f, vignette = 0.15f,
        ),
        Film(
            "fm2", Kind.ANALOG, "Nikon FM2", "Kodak Tri-X 400", "1982",
            "Bianco e nero duro e granuloso, da reportage.",
            Triple(0xFFF2C200, 0xFF111111, 0xFFF2C200),
            contrast = 0.45f, bw = true, fade = 0.02f, grain = 0.75f, vignette = 0.3f,
        ),
        Film(
            "m6", Kind.ANALOG, "Leica M6", "Ilford HP5 Plus", "1984",
            "Bianco e nero morbido, grigi lunghi e silenziosi.",
            Triple(0xFFEDEDED, 0xFF1E1E1E, 0xFF1E1E1E),
            contrast = 0.1f, bw = true, fade = 0.05f, roll = 0.03f, grain = 0.5f, vignette = 0.2f,
        ),
        Film(
            "sx70", Kind.ANALOG, "Polaroid SX-70", "Time-Zero", "1972",
            "Colori sbiaditi, ombre ciano, bordo bianco e quadrato.",
            Triple(0xFFE9E3D3, 0xFF5E8C61, 0xFF2B2B2B), iso = 150,
            exposure = 0.1f, contrast = -0.15f, saturation = 0.75f, warmth = 0.25f,
            shadows = Tint(-0.03f, 0.02f, 0.05f), highlights = Tint(0.05f, 0.03f, -0.02f),
            fade = 0.12f, roll = 0.08f, grain = 0.2f, vignette = 0.35f, frame = Frame.INSTANT,
        ),
        Film(
            "p600", Kind.ANALOG, "Polaroid 600", "Integral 600", "1981",
            "Istantanea più contrastata e fredda, flash in faccia.",
            Triple(0xFF1B1B1B, 0xFFE8442E, 0xFFF0EDE5), iso = 640,
            contrast = 0.2f, saturation = 0.9f, warmth = -0.15f, tint = 0.08f,
            shadows = Tint(-0.01f, 0.0f, 0.04f), highlights = Tint(0.02f, 0.0f, 0.02f),
            fade = 0.07f, roll = 0.05f, grain = 0.2f, vignette = 0.3f, frame = Frame.INSTANT,
        ),
        Film(
            "instax", Kind.ANALOG, "Fujifilm Instax Mini", "Instax Mini", "1998",
            "Luminosa, un filo fredda, cornice stretta e alta.",
            Triple(0xFFF8C8D8, 0xFFFFFFFF, 0xFF3A3A3A), iso = 800,
            exposure = 0.2f, contrast = 0.05f, saturation = 0.95f, warmth = -0.2f, tint = -0.05f,
            highlights = Tint(0.0f, 0.02f, 0.03f), fade = 0.05f, roll = 0.04f, grain = 0.15f,
            vignette = 0.15f, frame = Frame.INSTAX,
        ),
        Film(
            "instamatic", Kind.ANALOG, "Kodak Instamatic", "Kodacolor-X 126", "1963",
            "Quadrata, calda e consumata come un album dei nonni.",
            Triple(0xFFF2B705, 0xFF1A1A1A, 0xFFD3271C), iso = 64,
            contrast = -0.05f, saturation = 0.8f, warmth = 0.6f, tint = 0.1f,
            shadows = Tint(0.03f, 0.0f, 0.02f), highlights = Tint(0.05f, 0.02f, -0.06f),
            fade = 0.14f, roll = 0.1f, grain = 0.4f, vignette = 0.45f, frame = Frame.SQUARE,
        ),
        Film(
            "lca", Kind.ANALOG, "LOMO LC-A", "Agfa Precisa (cross)", "1984",
            "Colori sparati, vignettatura pesante, regola numero uno: non pensare.",
            Triple(0xFF2F6BB3, 0xFFE9D24A, 0xFF101010), iso = 100,
            contrast = 0.4f, saturation = 1.4f, warmth = 0.1f, tint = -0.1f,
            shadows = Tint(0.0f, 0.02f, 0.07f), highlights = Tint(0.05f, 0.05f, -0.06f),
            grain = 0.35f, vignette = 0.75f,
        ),
        Film(
            "holga", Kind.ANALOG, "Holga 120", "Fuji Pro 400H (120)", "1982",
            "Plastica, quadrata, con una fuga di luce che non perdona.",
            Triple(0xFF151515, 0xFF7FB7A4, 0xFFEFEFEF),
            contrast = 0.1f, saturation = 0.95f, warmth = 0.05f,
            shadows = Tint(0.0f, 0.03f, 0.03f), fade = 0.05f, grain = 0.45f, vignette = 0.85f,
            leak = 0.55f, frame = Frame.SQUARE,
        ),
        Film(
            "t4", Kind.ANALOG, "Yashica T4", "Fuji Superia 400", "1990",
            "Verdi profondi e ombre fredde, la compatta delle feste.",
            Triple(0xFF1E8C45, 0xFFE8E8E8, 0xFFB01E2E),
            contrast = 0.2f, saturation = 1.05f, warmth = -0.05f, tint = -0.12f,
            shadows = Tint(-0.02f, 0.03f, 0.02f), highlights = Tint(0.02f, 0.01f, 0.0f),
            fade = 0.04f, grain = 0.4f, vignette = 0.2f, date = true,
        ),
        Film(
            "k1000", Kind.ANALOG, "Pentax K1000", "Kodak Ektar 100", "1976",
            "Saturazione piena e grana finissima: paesaggi da cartolina.",
            Triple(0xFFE85D1C, 0xFF1A1A1A, 0xFFF5F0E6), iso = 100,
            contrast = 0.25f, saturation = 1.3f, warmth = 0.15f,
            highlights = Tint(0.02f, 0.0f, -0.02f), grain = 0.12f, vignette = 0.15f,
        ),
        Film(
            "t2", Kind.ANALOG, "Contax T2", "CineStill 800T", "1990",
            "Tungsteno: luci della notte fredde, neon con alone rosso.",
            Triple(0xFF141414, 0xFFC8102E, 0xFFEDEDED), iso = 800,
            contrast = 0.15f, saturation = 1.05f, warmth = -0.45f, tint = 0.05f,
            shadows = Tint(-0.02f, 0.02f, 0.06f), highlights = Tint(0.08f, 0.01f, -0.01f),
            fade = 0.05f, grain = 0.55f, vignette = 0.25f,
        ),
        Film(
            "funsaver", Kind.ANALOG, "Usa e getta", "Kodak FunSaver 800", "1988",
            "Flash, grana e la data arancione in basso: una festa del 1998.",
            Triple(0xFFF2B705, 0xFFE11F26, 0xFF111111), iso = 800,
            exposure = 0.1f, contrast = 0.3f, saturation = 1.1f, warmth = 0.35f,
            shadows = Tint(0.0f, 0.02f, 0.03f), fade = 0.03f, grain = 0.65f, vignette = 0.45f, date = true,
        ),
        Film(
            "x700", Kind.ANALOG, "Minolta X-700", "Fujichrome Velvia 50", "1981",
            "Diapositiva: blu profondi, rossi pieni, neri chiusi.",
            Triple(0xFF2C5E35, 0xFFE9E3D3, 0xFF2C5E35), iso = 50,
            exposure = -0.1f, contrast = 0.45f, saturation = 1.45f, tint = 0.06f,
            shadows = Tint(0.0f, 0.0f, 0.03f), grain = 0.1f, vignette = 0.3f,
        ),
        Film(
            "ql17", Kind.ANALOG, "Canonet QL17", "Kodak Ektachrome E100", "1972",
            "Pulita e fredda, cieli turchesi e bianchi netti.",
            Triple(0xFF1D3F8C, 0xFFF2F2F2, 0xFFE0A030), iso = 100,
            contrast = 0.2f, saturation = 1.1f, warmth = -0.25f,
            shadows = Tint(-0.02f, 0.02f, 0.05f), highlights = Tint(0.0f, 0.01f, 0.02f),
            grain = 0.15f, vignette = 0.2f,
        ),
        Film(
            "pop", Kind.ANALOG, "Konica Pop", "Pellicola scaduta", "1985",
            "Rullino ritrovato in un cassetto: magenta, slavato, imprevedibile.",
            Triple(0xFF9A3B78, 0xFFE9E3D3, 0xFF1A1A1A), iso = 200,
            exposure = 0.15f, contrast = -0.2f, saturation = 0.85f, warmth = 0.1f, tint = 0.35f,
            shadows = Tint(0.04f, -0.02f, 0.05f), highlights = Tint(0.04f, 0.02f, -0.03f),
            fade = 0.16f, roll = 0.08f, grain = 0.6f, vignette = 0.35f, leak = 0.25f, frame = Frame.REBATE,
        ),
        Film(
            "zenit", Kind.ANALOG, "Zenit-E", "Svema Foto 64", "1965",
            "Pellicola sovietica: toni bruni, verdi spenti, tanta grana.",
            Triple(0xFF8C1C13, 0xFFD9C8A0, 0xFF1A1A1A), iso = 64,
            contrast = 0.1f, saturation = 0.6f, warmth = 0.45f, tint = 0.05f,
            shadows = Tint(0.03f, 0.01f, -0.02f), highlights = Tint(0.04f, 0.02f, -0.05f),
            fade = 0.1f, roll = 0.06f, grain = 0.7f, vignette = 0.5f, frame = Frame.REBATE,
        ),
        Film(
            "brownie", Kind.ANALOG, "Kodak Brownie", "Verichrome Pan", "1950",
            "Monocromo seppia, quadrato e morbido come una foto di famiglia.",
            Triple(0xFF3A2A1A, 0xFFD9B26F, 0xFF3A2A1A), iso = 125,
            contrast = 0.05f, bw = true, shadows = Tint(0.03f, 0.01f, -0.02f), highlights = Tint(0.06f, 0.03f, -0.04f),
            fade = 0.12f, roll = 0.1f, grain = 0.5f, vignette = 0.6f, frame = Frame.SQUARE,
        ),
    )

    val cinema = listOf(
        Film(
            "golden", Kind.CINEMA, "Golden Hour", "Kodak Vision3 250D", "Ora d'oro",
            "Neri profondi, luce calda radente, cieli verde acqua.",
            Triple(0xFFE8642C, 0xFF1F3A3D, 0xFFF0E2C8), iso = 250,
            exposure = -0.2f, contrast = 0.3f, saturation = 0.85f, warmth = 0.25f,
            shadows = Tint(-0.03f, 0.01f, 0.03f), highlights = Tint(0.05f, 0.02f, -0.04f),
            fade = 0.03f, roll = 0.06f, grain = 0.18f, vignette = 0.3f,
        ),
        Film(
            "teal", Kind.CINEMA, "Teal & Orange", "Blockbuster", "Azione",
            "Pelle calda contro ombre ottanio: il look dei film d'estate.",
            Triple(0xFF0F5257, 0xFFE8642C, 0xFFEDEDED),
            contrast = 0.35f, saturation = 1.1f, warmth = 0.1f,
            shadows = Tint(-0.05f, 0.02f, 0.06f), highlights = Tint(0.06f, 0.02f, -0.05f),
            roll = 0.03f, grain = 0.1f, vignette = 0.2f,
        ),
        Film(
            "moody", Kind.CINEMA, "Moody", "Kodak 2383 Print", "Indie",
            "Desaturato e scuro, ombre fredde, luci color crema.",
            Triple(0xFF2B3A3F, 0xFFC9B48A, 0xFF101010),
            exposure = -0.25f, contrast = 0.2f, saturation = 0.72f, warmth = 0.05f,
            shadows = Tint(-0.02f, 0.015f, 0.035f), highlights = Tint(0.03f, 0.02f, -0.02f),
            fade = 0.05f, roll = 0.08f, grain = 0.22f, vignette = 0.35f,
        ),
        Film(
            "bleach", Kind.CINEMA, "Bleach Bypass", "Sviluppo senza sbianca", "Guerra",
            "Argento rimasto nella pellicola: duro, metallico, quasi senza colore.",
            Triple(0xFF7A7F80, 0xFF202020, 0xFFE0E0E0),
            contrast = 0.55f, saturation = 0.45f, warmth = -0.05f,
            shadows = Tint(0.0f, 0.01f, 0.02f), grain = 0.3f, vignette = 0.3f,
        ),
        Film(
            "noir", Kind.CINEMA, "Noir", "Double-X 5222", "Anni '40",
            "Bianco e nero da cinema: neri d'inchiostro, luce scolpita.",
            Triple(0xFF0A0A0A, 0xFFEDEDED, 0xFF0A0A0A), iso = 250,
            exposure = -0.1f, contrast = 0.6f, bw = true, roll = 0.02f, grain = 0.4f, vignette = 0.45f,
        ),
        Film(
            "night", Kind.CINEMA, "Day for Night", "Notte americana", "Western",
            "Il giorno girato come se fosse notte: blu, scuro, lunare.",
            Triple(0xFF0B1F3A, 0xFF7FA6D9, 0xFFDDE6F2),
            exposure = -0.9f, contrast = 0.25f, saturation = 0.45f, warmth = -0.9f,
            shadows = Tint(-0.02f, 0.0f, 0.05f), highlights = Tint(-0.02f, 0.01f, 0.05f),
            grain = 0.2f, vignette = 0.5f,
        ),
        Film(
            "matrix", Kind.CINEMA, "Codice Verde", "Neon digitale", "Sci-fi",
            "Dominante verde e contrasto freddo, da mondo simulato.",
            Triple(0xFF0E2A12, 0xFF52E07A, 0xFF0E2A12),
            contrast = 0.35f, saturation = 0.8f, tint = -0.45f,
            shadows = Tint(-0.02f, 0.03f, 0.0f), highlights = Tint(0.0f, 0.03f, -0.02f),
            grain = 0.15f, vignette = 0.3f,
        ),
        Film(
            "pastel", Kind.CINEMA, "Pastello", "Commedia simmetrica", "Fiaba",
            "Rosa e menta, ombre aperte: tutto sembra una scenografia.",
            Triple(0xFFF2B8C6, 0xFF9ED9C8, 0xFF5A3A4A),
            exposure = 0.15f, contrast = -0.1f, saturation = 0.9f, warmth = 0.15f, tint = 0.15f,
            shadows = Tint(0.03f, 0.02f, 0.04f), highlights = Tint(0.02f, 0.0f, 0.0f),
            fade = 0.1f, roll = 0.03f, grain = 0.08f, vignette = 0.1f,
        ),
    )

    val all = analog + cinema

    fun byId(id: String?): Film? = all.firstOrNull { it.id == id }
}

/** Rapporti d'aspetto per il cinema (lato lungo / lato corto). */
enum class Ratio(val label: String, val value: Float?) {
    OPEN("Open", null),
    HD("16:9", 16f / 9f),
    FLAT("1.85", 1.85f),
    SCOPE("2.39", 2.39f),
}

/**
 * Il colore della pellicola, pixel per pixel. Lavora su componenti 0..1 e scrive in [out].
 * Tiene solo la parte "chimica": grana, vignettatura e cornici sono in [Darkroom].
 */
class Grade(private val film: Film, private val strength: Float = 1f) {
    private val gain = 2f.pow(film.exposure)
    private val rGain = 1f + 0.12f * film.warmth + 0.03f * film.tint
    private val gGain = 1f - 0.06f * film.tint
    private val bGain = 1f - 0.14f * film.warmth + 0.03f * film.tint
    private val k = film.contrast
    private val span = 1f - film.fade - film.roll

    fun apply(r0: Float, g0: Float, b0: Float, out: FloatArray) {
        var r = r0 * gain * rGain
        var g = g0 * gain * gGain
        var b = b0 * gain * bGain
        var l = 0.2126f * r + 0.7152f * g + 0.0722f * b
        if (film.bw) {
            // Pancromatica: un po' più sensibile al rosso, come le pellicole vere.
            val m = 0.3f * r + 0.6f * g + 0.1f * b
            r = m; g = m; b = m
            l = m
        } else {
            val s = film.saturation
            r = l + (r - l) * s; g = l + (g - l) * s; b = l + (b - l) * s
        }
        r = curve(r); g = curve(g); b = curve(b)
        l = (0.2126f * r + 0.7152f * g + 0.0722f * b).coerceIn(0f, 1f)
        val sw = (1f - l) * (1f - l)
        val hw = l * l
        r += film.shadows.r * sw + film.highlights.r * hw
        g += film.shadows.g * sw + film.highlights.g * hw
        b += film.shadows.b * sw + film.highlights.b * hw
        r = film.fade + r.coerceIn(0f, 1f) * span
        g = film.fade + g.coerceIn(0f, 1f) * span
        b = film.fade + b.coerceIn(0f, 1f) * span
        out[0] = r0 + (r - r0) * strength
        out[1] = g0 + (g - g0) * strength
        out[2] = b0 + (b - b0) * strength
    }

    /** Curva a S (contrasto positivo) o appiattita (negativo), con spalla morbida sulle alte luci. */
    private fun curve(x0: Float): Float {
        var x = x0.coerceAtLeast(0f)
        // Spalla: le alte luci bruciate rientrano dolcemente invece di tagliarsi.
        if (x > 0.8f) x = 0.8f + (1f - 1f / (1f + (x - 0.8f) * 5f)) * 0.2f
        x = x.coerceAtMost(1f)
        return if (k >= 0f) x + (x * x * (3f - 2f * x) - x) * k
        else 0.5f + (x - 0.5f) * (1f + k)
    }
}
