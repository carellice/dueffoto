package app.dueffoto

import kotlin.math.pow

/**
 * Analogico: emula una fotocamera con la sua pellicola. Moody: il look scuro e cinematografico delle foto
 * da telefono "fatte bene". Cinema: color grading da pellicola cinematografica.
 */
enum class Kind(val label: String) { ANALOG("ANALOGICO"), MOODY("MOODY"), CINEMA("CINEMA") }

/** Scaffali del frigo: come si raggruppano le pellicole nella scelta. */
enum class Family(val label: String) {
    COLOR("Colore"), BW("Bianco e nero"), SLIDE("Diapositive"), INSTANT("Istantanee"), LOMO("Sperimentali"),
    MOODY("Moody"), CINEMA("Cinema"),
}

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
    /** Ombre chiuse: schiaccia i neri senza toccare le luci (0..1.5). */
    val toe: Float = 0f,
    /** Saturazione residua nelle ombre profonde (1 = come il resto). */
    val shadowSat: Float = 1f,
    /** Verdi spenti verso l'oliva (0..1). */
    val greens: Float = 0f,
    /** Blu di cieli e mare verso un ottanio grigio (0..1). */
    val blues: Float = 0f,
    val mids: Tint = Tint(),
) {
    val family: Family
        get() = when {
            kind == Kind.MOODY -> Family.MOODY
            kind == Kind.CINEMA -> Family.CINEMA
            bw -> Family.BW
            frame == Frame.INSTANT || frame == Frame.INSTAX -> Family.INSTANT
            id in setOf("x700", "ql17") -> Family.SLIDE
            id in setOf("lca", "holga", "pop", "purple", "zenit") -> Family.LOMO
            else -> Family.COLOR
        }

    /** L'illustrazione del rullino (o della scatola, della latta, del filtro). */
    val art: Int get() = Films.art(id)
}

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
        Film(
            "mju2", Kind.ANALOG, "Olympus µ-II", "Fujicolor C200", "1997",
            "La compatta tascabile degli anni '90: verdi freschi, flash sempre pronto.",
            Triple(0xFF6FA84A, 0xFFF2F2EA, 0xFF1C5C2E), iso = 200,
            contrast = 0.2f, saturation = 1.0f, warmth = -0.1f, tint = -0.1f,
            shadows = Tint(-0.02f, 0.02f, 0.02f), highlights = Tint(0.01f, 0.01f, 0.0f),
            fade = 0.03f, grain = 0.35f, vignette = 0.3f,
        ),
        Film(
            "gr1", Kind.ANALOG, "Ricoh GR1", "Agfa Vista 200", "1996",
            "Rossi pieni e bianchi puliti: la compatta dei fotografi di strada.",
            Triple(0xFFF2EEE6, 0xFFD52B2B, 0xFF1A1A1A), iso = 200,
            contrast = 0.25f, saturation = 1.15f, warmth = 0.25f, tint = 0.08f,
            highlights = Tint(0.03f, 0.0f, -0.03f), grain = 0.3f, vignette = 0.2f,
        ),
        Film(
            "sureshot", Kind.ANALOG, "Canon Sure Shot", "Kodak Ultramax 400", "1979",
            "Caldo, saturo, indistruttibile: la pellicola di tutti i compleanni.",
            Triple(0xFF1C3A7A, 0xFFF2C21B, 0xFF0E1A3A),
            contrast = 0.25f, saturation = 1.15f, warmth = 0.4f,
            shadows = Tint(0.0f, 0.01f, 0.02f), highlights = Tint(0.03f, 0.02f, -0.04f),
            fade = 0.02f, grain = 0.4f, vignette = 0.25f,
        ),
        Film(
            "f3", Kind.ANALOG, "Nikon F3", "Ilford Delta 3200", "1980",
            "Bianco e nero da notte: grana enorme, neri morbidi, luce che non c'è.",
            Triple(0xFF151515, 0xFFF0F0F0, 0xFFE8642C), iso = 3200,
            exposure = 0.1f, contrast = 0.2f, bw = true, fade = 0.06f, grain = 0.95f, vignette = 0.3f,
        ),
        Film(
            "rz67", Kind.ANALOG, "Mamiya RZ67", "Kodak Portra 160 (120)", "1982",
            "Medio formato 6×7: pelle perfetta, pastelli e una nitidezza da studio.",
            Triple(0xFFF2EEE6, 0xFFE07B1F, 0xFF1C2F5A), iso = 160,
            contrast = -0.1f, saturation = 0.9f, warmth = 0.2f,
            shadows = Tint(0.0f, 0.01f, 0.02f), highlights = Tint(0.02f, 0.01f, -0.02f),
            fade = 0.04f, roll = 0.03f, grain = 0.12f, vignette = 0.1f, aspect = 7f / 6f,
        ),
        Film(
            "purple", Kind.ANALOG, "LOMO LC-Wide", "LomoChrome Purple", "2013",
            "I verdi diventano viola, il cielo turchese: un altro pianeta.",
            Triple(0xFF4B1F6E, 0xFFC9A2E8, 0xFF1A0A2A),
            contrast = 0.25f, saturation = 1.1f, tint = 0.5f,
            shadows = Tint(0.05f, -0.03f, 0.06f), highlights = Tint(0.02f, -0.02f, 0.04f),
            grain = 0.4f, vignette = 0.5f, greens = 1f,
        ),
        Film(
            "pentax17", Kind.ANALOG, "Pentax 17", "Harman Phoenix 200", "2024",
            "Mezzo formato verticale, rossi accesi e aloni: la pellicola ribelle.",
            Triple(0xFFD2401E, 0xFFF0E2C8, 0xFF2A1A0A), iso = 200,
            contrast = 0.35f, saturation = 1.1f, warmth = 0.3f, tint = 0.1f,
            highlights = Tint(0.06f, 0.0f, -0.03f), grain = 0.55f, vignette = 0.3f, leak = 0.15f, aspect = 1.5f,
        ),
        Film(
            "colorplus", Kind.ANALOG, "Minolta Riva", "Kodak ColorPlus 200", "1988",
            "Economica e calda, un po' slavata: le estati in spiaggia.",
            Triple(0xFFF2C21B, 0xFFE8642C, 0xFF2A1A0A), iso = 200,
            saturation = 0.9f, warmth = 0.45f, shadows = Tint(0.02f, 0.01f, -0.01f),
            fade = 0.05f, roll = 0.03f, grain = 0.4f, vignette = 0.3f, date = true,
        ),
    )

    /** Il look delle foto da telefono "cinematiche": calibrato su foto di riferimento (tools/calibrate.py). */
    val moody = listOf(
        Film(
            "dusk", Kind.MOODY, "Dusk", "Strade al tramonto", "Crepuscolo",
            "Cielo grigio ottanio, nuvole color pesca, asfalto che sparisce nel buio.",
            Triple(0xFF4F6E73, 0xFFF2A07A, 0xFF101010),
            exposure = -0.1f, contrast = 0.15f, saturation = 0.5f, warmth = 0.3f, toe = 0.9f, roll = 0.3f, shadowSat = 0.6f,
            greens = 0.5f, blues = 0.9f, shadows = Tint(-0.03f, -0.01f, 0f), mids = Tint(0.03f, 0.01f, -0.04f),
            highlights = Tint(0.12f, 0.01f, -0.12f), grain = 0.12f, vignette = 0.2f,
        ),
        Film(
            "oak", Kind.MOODY, "Oak Shade", "Ombra dorata", "Sottobosco",
            "Ombre profonde, foglie oliva e lame di luce dorata.",
            Triple(0xFF3A3020, 0xFFE8B04A, 0xFF101010),
            exposure = -0.6f, contrast = 0.15f, saturation = 0.5f, warmth = 0.45f, toe = 1.1f, roll = 0.3f, shadowSat = 0.6f,
            greens = 1f, blues = 0.7f, shadows = Tint(-0.02f, -0.015f, -0.02f), mids = Tint(0.01f, 0.005f, -0.02f),
            highlights = Tint(0.13f, 0.07f, -0.09f), grain = 0.12f, vignette = 0.25f,
        ),
        Film(
            "canyon", Kind.MOODY, "Canyon", "Colline bruciate", "Ora d'oro",
            "Terra arancione, ombre che tagliano, cielo azzurro polvere.",
            Triple(0xFFB5542A, 0xFF5FB3B8, 0xFF101010),
            exposure = -0.2f, contrast = 0.15f, saturation = 0.75f, warmth = 0.4f, tint = 0.05f, toe = 0.9f, roll = 0.3f,
            shadowSat = 0.6f, greens = 0.7f, blues = 0.4f, shadows = Tint(-0.02f, 0f, 0.02f), mids = Tint(0.01f, 0.005f, -0.02f),
            highlights = Tint(0.12f, 0.03f, -0.12f), grain = 0.12f, vignette = 0.2f,
        ),
        Film(
            "lastlight", Kind.MOODY, "Last Light", "Ultima luce", "Blu notte",
            "Tutto è sagoma tranne una riga arancione all'orizzonte.",
            Triple(0xFF141C2E, 0xFFF08A2C, 0xFF0A0A0A),
            exposure = -0.5f, contrast = 0.35f, saturation = 0.5f, toe = 1.2f, roll = 0.3f, shadowSat = 0.6f,
            greens = 0.5f, blues = 0.7f, shadows = Tint(-0.02f, 0f, 0.015f), mids = Tint(0.01f, 0.005f, -0.02f),
            highlights = Tint(0.15f, 0.04f, -0.12f), grain = 0.15f, vignette = 0.3f,
        ),
        Film(
            "tealroom", Kind.MOODY, "Teal Room", "Luce di finestra", "Interni",
            "Pareti ottanio, pelle calda, il sole che entra di taglio.",
            Triple(0xFF1F4E4A, 0xFFE0A050, 0xFF101010),
            exposure = -0.2f, contrast = 0.15f, saturation = 0.6f, warmth = 0.15f, tint = -0.05f, toe = 0.9f, roll = 0.3f,
            shadowSat = 0.6f, greens = 0.25f, blues = 0.9f, shadows = Tint(-0.03f, 0.012f, 0f), mids = Tint(-0.035f, 0.02f, -0.015f),
            highlights = Tint(0.08f, 0.04f, -0.10f), grain = 0.12f, vignette = 0.2f,
        ),
        Film(
            "coast", Kind.MOODY, "Coastline", "Costa d'autunno", "Mare",
            "Mare d'ardesia, cielo verde acqua, orizzonte color crema.",
            Triple(0xFF4A6E80, 0xFFF0DFC0, 0xFF101010),
            exposure = -0.15f, contrast = 0.15f, saturation = 0.42f, warmth = 0.1f, toe = 0.8f, roll = 0.32f, shadowSat = 0.6f,
            greens = 0.7f, blues = 0.8f, shadows = Tint(-0.03f, -0.01f, 0f), mids = Tint(-0.04f, 0.005f, -0.01f),
            highlights = Tint(0.12f, 0.05f, -0.10f), grain = 0.1f, vignette = 0.2f,
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
            "moody", Kind.CINEMA, "Indie Print", "Kodak 2383 Print", "Indie",
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
        Film(
            "vision500", Kind.CINEMA, "Neon Night", "Kodak Vision3 500T", "Notte",
            "Tungsteno di notte: lampioni con l'alone rosso, ombre blu petrolio.",
            Triple(0xFF1A1F3A, 0xFFC8102E, 0xFF2F6BB3), iso = 500,
            contrast = 0.2f, saturation = 0.95f, warmth = -0.5f, toe = 0.3f,
            shadows = Tint(-0.02f, 0.02f, 0.05f), highlights = Tint(0.07f, 0.01f, -0.01f),
            roll = 0.04f, grain = 0.3f, vignette = 0.3f,
        ),
        Film(
            "techni", Kind.CINEMA, "Tre Strisce", "Technicolor", "Hollywood '50",
            "Colori densi come vernice: rossi da musical e cieli da cartolina.",
            Triple(0xFFF2C21B, 0xFFC8102E, 0xFF1D3F8C),
            contrast = 0.35f, saturation = 1.35f, warmth = 0.15f,
            shadows = Tint(0f, 0f, 0.03f), highlights = Tint(0.03f, 0.01f, -0.02f), grain = 0.15f, vignette = 0.25f,
        ),
        Film(
            "super8", Kind.CINEMA, "Super 8", "Kodachrome 40 (S8)", "Anni '70",
            "Il filmino delle vacanze: caldo, sgranato, con la luce che entra dai bordi.",
            Triple(0xFFF2C21B, 0xFF1A1A1A, 0xFFE8642C), iso = 40,
            contrast = 0.2f, saturation = 1.05f, warmth = 0.35f, fade = 0.06f, roll = 0.08f,
            grain = 0.6f, vignette = 0.55f, leak = 0.2f, aspect = 4f / 3f,
        ),
    )

    val all = analog + moody + cinema

    fun byId(id: String?): Film? = all.firstOrNull { it.id == id }

    fun of(kind: Kind) = when (kind) {
        Kind.ANALOG -> analog
        Kind.MOODY -> moody
        Kind.CINEMA -> cinema
    }

    fun art(id: String): Int = when (id) {
        "trip35" -> R.drawable.film_trip35
        "ae1" -> R.drawable.film_ae1
        "fm2" -> R.drawable.film_fm2
        "m6" -> R.drawable.film_m6
        "sx70" -> R.drawable.film_sx70
        "p600" -> R.drawable.film_p600
        "instax" -> R.drawable.film_instax
        "instamatic" -> R.drawable.film_instamatic
        "lca" -> R.drawable.film_lca
        "holga" -> R.drawable.film_holga
        "t4" -> R.drawable.film_t4
        "k1000" -> R.drawable.film_k1000
        "t2" -> R.drawable.film_t2
        "funsaver" -> R.drawable.film_funsaver
        "x700" -> R.drawable.film_x700
        "ql17" -> R.drawable.film_ql17
        "pop" -> R.drawable.film_pop
        "zenit" -> R.drawable.film_zenit
        "brownie" -> R.drawable.film_brownie
        "mju2" -> R.drawable.film_mju2
        "gr1" -> R.drawable.film_gr1
        "sureshot" -> R.drawable.film_sureshot
        "f3" -> R.drawable.film_f3
        "rz67" -> R.drawable.film_rz67
        "purple" -> R.drawable.film_purple
        "pentax17" -> R.drawable.film_pentax17
        "colorplus" -> R.drawable.film_colorplus
        "dusk" -> R.drawable.film_dusk
        "oak" -> R.drawable.film_oak
        "canyon" -> R.drawable.film_canyon
        "lastlight" -> R.drawable.film_lastlight
        "tealroom" -> R.drawable.film_tealroom
        "coast" -> R.drawable.film_coast
        "golden" -> R.drawable.film_golden
        "teal" -> R.drawable.film_teal
        "moody" -> R.drawable.film_moody
        "bleach" -> R.drawable.film_bleach
        "noir" -> R.drawable.film_noir
        "night" -> R.drawable.film_night
        "matrix" -> R.drawable.film_matrix
        "pastel" -> R.drawable.film_pastel
        "vision500" -> R.drawable.film_vision500
        "techni" -> R.drawable.film_techni
        "super8" -> R.drawable.film_super8
        else -> R.drawable.film_trip35
    }
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
    private val toe = film.toe
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
            // Le ombre profonde perdono colore prima del resto, come sulla pellicola.
            val s = film.saturation * (film.shadowSat + (1f - film.shadowSat) * smooth(0.03f, 0.4f, l))
            r = l + (r - l) * s; g = l + (g - l) * s; b = l + (b - l) * s
            if (film.greens > 0f) {
                val gx = (g - maxOf(r, b)).coerceAtLeast(0f)
                g -= film.greens * gx * 0.8f
                r += film.greens * gx * 0.3f
            }
            if (film.blues > 0f) {
                val bx = (b - maxOf(r, g)).coerceAtLeast(0f)
                b -= film.blues * bx * 0.6f
                g += film.blues * bx * 0.35f
            }
        }
        r = curve(r); g = curve(g); b = curve(b)
        l = (0.2126f * r + 0.7152f * g + 0.0722f * b).coerceIn(0f, 1f)
        val sw = (1f - l) * (1f - l)
        val hw = l * l
        val mw = 1f - sw - hw
        r += film.shadows.r * sw + film.mids.r * mw + film.highlights.r * hw
        g += film.shadows.g * sw + film.mids.g * mw + film.highlights.g * hw
        b += film.shadows.b * sw + film.mids.b * mw + film.highlights.b * hw
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
        x = if (k >= 0f) x + (x * x * (3f - 2f * x) - x) * k
        else 0.5f + (x - 0.5f) * (1f + k)
        x = x.coerceIn(0f, 1f)
        // Piede: i neri si chiudono, le luci restano dove sono.
        return if (toe > 0f) x.pow(1f + toe * (1f - x)) else x
    }

    private fun smooth(e0: Float, e1: Float, x: Float): Float {
        val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}
