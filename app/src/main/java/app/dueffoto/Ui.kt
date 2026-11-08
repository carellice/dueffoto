package app.dueffoto

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Colori del corpo macchina: similpelle, cromature, ottone e l'arancione delle scritte sul bordo pellicola. */
object Ink {
    val body = Color(0xFF14110E)
    val leather = Color(0xFF1E1A16)
    val panel = Color(0xFF26211C)
    val chrome = Color(0xFFCFC8BA)
    val chromeDark = Color(0xFF6E685E)
    val cream = Color(0xFFF0E6D2)
    val dim = Color(0xFF9C9284)
    val faint = Color(0xFF5A5248)
    val orange = Color(0xFFE8642C)
    val amber = Color(0xFFFFB347)
    val brass = Color(0xFFC8A15A)
    val rec = Color(0xFFE23B2E)
    val teal = Color(0xFF4FA3A0)
}

object Type {
    val serif = FontFamily(
        Font(R.font.serif_display),
        Font(R.font.serif_display_italic, style = FontStyle.Italic),
    )
    val mono = FontFamily(
        Font(R.font.mono),
        Font(R.font.mono_bold, weight = FontWeight.Bold),
    )
    val title = TextStyle(fontFamily = serif, fontSize = 28.sp, color = Ink.cream)
    val name = TextStyle(fontFamily = serif, fontSize = 15.sp, color = Ink.cream, lineHeight = 17.sp)
    val label = TextStyle(fontFamily = mono, fontSize = 10.sp, letterSpacing = 1.5.sp, color = Ink.dim)
    val body = TextStyle(fontFamily = serif, fontStyle = FontStyle.Italic, fontSize = 15.sp, color = Ink.dim, lineHeight = 19.sp)
}

@Composable
fun DueffotoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Ink.orange, onPrimary = Ink.body, background = Ink.body, surface = Ink.leather,
            onSurface = Ink.cream, onBackground = Ink.cream, secondary = Ink.brass,
        ),
        content = content,
    )
}

/** Pressione "a molla": tutti i comandi rientrano un po' quando li tocchi. */
@Composable
fun Modifier.press(onClick: () -> Unit, enabled: Boolean = true): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.9f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "press")
    return this.scale(s).clickable(source, null, enabled = enabled, onClick = onClick)
}

/** Il contatore a cristalli liquidi delle compatte: le pose scattate. */
@Composable
fun Lcd(text: String, caption: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0B0F0A))
            .border(1.dp, Color(0xFF2C3328), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(caption, style = Type.label.copy(fontSize = 8.sp, color = Ink.amber.copy(alpha = 0.55f)))
        Spacer(Modifier.width(6.dp))
        Text(text, style = TextStyle(fontFamily = Type.mono, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink.amber, letterSpacing = 2.sp))
    }
}

/** Un interruttore a due o più posizioni, inciso nel corpo macchina. */
@Composable
fun <T> Switch(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Ink.leather)
            .border(1.dp, Ink.panel, RoundedCornerShape(50))
            .padding(3.dp),
    ) {
        options.forEach { (value, label) ->
            val on = value == selected
            val bg by animateColorAsState(if (on) Ink.chrome else Color.Transparent, label = "switch")
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(bg)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = Type.label.copy(color = if (on) Ink.body else Ink.dim, fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
fun Chip(text: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (on) Ink.orange.copy(alpha = 0.18f) else Color.Transparent)
            .border(1.dp, if (on) Ink.orange else Ink.panel, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text, style = Type.label.copy(color = if (on) Ink.orange else Ink.dim, fontWeight = FontWeight.Bold))
    }
}

/** Una ghiera con tacche, come quella dei tempi: etichetta, valore e cursore. */
@Composable
/** [bipolar]: lo zero sta al centro (esposizione), quindi niente traccia "piena" da sinistra. */
fun Dial(label: String, value: Float, range: ClosedFloatingPointRange<Float>, display: String, onChange: (Float) -> Unit, modifier: Modifier = Modifier, bipolar: Boolean = false) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = Type.label, modifier = Modifier.width(44.dp))
        Slider(
            value = value, onValueChange = onChange, valueRange = range,
            modifier = Modifier.weight(1f).height(28.dp),
            colors = SliderDefaults.colors(
                thumbColor = Ink.chrome, activeTrackColor = if (bipolar) Ink.panel else Ink.orange, inactiveTrackColor = Ink.panel,
                activeTickColor = Ink.faint, inactiveTickColor = Ink.faint,
            ),
        )
        Text(display, style = Type.label.copy(color = Ink.cream), textAlign = TextAlign.End, modifier = Modifier.width(44.dp))
    }
}

/** Il rullino della pellicola, disegnato: corpo colorato, coperchi cromati e la linguetta che esce. */
@Composable
fun Canister(film: Film?, selected: Boolean, onClick: () -> Unit) {
    val lift by animateFloatAsState(if (selected) 1f else 0f, spring(Spring.DampingRatioMediumBouncy), label = "lift")
    Column(
        Modifier
            .width(96.dp)
            .press(onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(width = 86.dp, height = 92.dp)
                .padding(top = (10 - 10 * lift).coerceIn(0f, 10f).dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                if (selected) {
                    drawCircle(Brush.radialGradient(listOf(Ink.orange.copy(alpha = 0.35f), Color.Transparent)), radius = size.minDimension * 0.62f)
                }
                drawCanister(film)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            film?.camera ?: "Scegli dopo", style = Type.name.copy(fontSize = 13.sp, color = if (selected) Ink.cream else Ink.dim),
            maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis,
        )
        Text(
            (film?.stock ?: "camera oscura").uppercase(),
            style = Type.label.copy(fontSize = 8.sp, letterSpacing = 0.8.sp, color = if (selected) Ink.orange else Ink.faint),
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
    }
}

private fun DrawScope.drawCanister(film: Film?) {
    val (bodyC, bandC, inkC) = film?.canister?.let { Triple(Color(it.first), Color(it.second), Color(it.third)) }
        ?: Triple(Ink.panel, Ink.leather, Ink.dim)
    val w = size.width * 0.5f
    val h = size.height * 0.62f
    val left = size.width * 0.12f
    val top = (size.height - h) / 2f
    // Linguetta della pellicola con le perforazioni.
    val tongueTop = top + h * 0.3f
    val tongueH = h * 0.42f
    drawRoundRect(Color(0xFF3A2414), Offset(left + w * 0.6f, tongueTop), Size(size.width * 0.78f - w * 0.6f, tongueH), CornerRadius(3f))
    var x = left + w + 4f
    while (x < size.width * 0.84f) {
        drawRect(Color(0xFF0E0906), Offset(x, tongueTop + 3f), Size(4f, 4f))
        drawRect(Color(0xFF0E0906), Offset(x, tongueTop + tongueH - 7f), Size(4f, 4f))
        x += 9f
    }
    // Coperchi cromati sopra e sotto.
    val cap = Brush.horizontalGradient(listOf(Ink.chromeDark, Ink.chrome, Color.White, Ink.chrome, Ink.chromeDark), startX = left, endX = left + w)
    drawRoundRect(cap, Offset(left + w * 0.06f, top - 7f), Size(w * 0.88f, 9f), CornerRadius(3f))
    drawRoundRect(cap, Offset(left + w * 0.06f, top + h - 2f), Size(w * 0.88f, 9f), CornerRadius(3f))
    drawRoundRect(cap, Offset(left + w * 0.34f, top - 13f), Size(w * 0.32f, 7f), CornerRadius(2f))
    // Corpo cilindrico: colore pieno con riflesso e ombra laterale.
    drawRoundRect(bodyC, Offset(left, top), Size(w, h), CornerRadius(4f))
    drawRect(bandC, Offset(left, top + h * 0.38f), Size(w, h * 0.26f))
    drawRect(inkC.copy(alpha = 0.9f), Offset(left + w * 0.18f, top + h * 0.47f), Size(w * 0.64f, h * 0.035f))
    drawRect(inkC.copy(alpha = 0.6f), Offset(left + w * 0.18f, top + h * 0.53f), Size(w * 0.4f, h * 0.025f))
    drawRoundRect(
        Brush.horizontalGradient(
            listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent, Color.White.copy(alpha = 0.28f), Color.Transparent, Color.Black.copy(alpha = 0.55f)),
            startX = left, endX = left + w,
        ),
        Offset(left, top), Size(w, h), CornerRadius(4f),
    )
    if (film == null) {
        drawCircle(Ink.dim, radius = w * 0.16f, center = Offset(left + w / 2f, top + h * 0.2f), style = Stroke(2f))
    }
}

/** Il pulsante di scatto: ghiera zigrinata cromata e bottone crema (rosso per i video). */
@Composable
fun ShutterButton(video: Boolean, recording: Boolean, enabled: Boolean, onClick: () -> Unit, size: Dp = 84.dp) {
    val inner by animateFloatAsState(if (recording) 0.42f else 0.72f, spring(Spring.DampingRatioMediumBouncy), label = "inner")
    val corner by animateFloatAsState(if (recording) 0.25f else 1f, label = "corner")
    Box(Modifier.size(size).press(onClick, enabled), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2f
            drawCircle(Brush.sweepGradient(listOf(Ink.chrome, Ink.chromeDark, Color.White, Ink.chromeDark, Ink.chrome)), r)
            // Zigrinatura.
            for (i in 0 until 60) {
                rotate(i * 6f) {
                    drawLine(Color.Black.copy(alpha = 0.35f), Offset(center.x, center.y - r), Offset(center.x, center.y - r * 0.88f), 2f)
                }
            }
            drawCircle(Ink.body, r * 0.84f)
            val s = r * inner * 2f
            val color = if (video) Ink.rec else Ink.cream
            drawRoundRect(
                Brush.radialGradient(listOf(color, color.copy(alpha = 0.85f)), center = center, radius = s),
                Offset(center.x - s / 2f, center.y - s / 2f), Size(s, s), CornerRadius(s / 2f * corner),
            )
        }
    }
}

/** Bottone rotondo secondario (galleria, fotocamera frontale, flash...). */
@Composable
fun RoundButton(onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 52.dp, content: @Composable () -> Unit) {
    Box(
        modifier
            .size(size)
            .press(onClick)
            .clip(CircleShape)
            .background(Ink.leather)
            .border(1.dp, Ink.panel, CircleShape),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun Wordmark(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(Ink.orange))
        Text("Dueffoto", style = Type.title.copy(fontStyle = FontStyle.Italic, fontSize = 24.sp))
    }
}

@Composable
fun FilmStripDivider(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(10.dp)) {
        drawRect(Color(0xFF0E0B08))
        var x = 6f
        while (x < size.width) {
            drawRoundRect(Ink.panel, Offset(x, size.height * 0.25f), Size(10f, size.height * 0.5f), CornerRadius(2f))
            x += 22f
        }
    }
}
