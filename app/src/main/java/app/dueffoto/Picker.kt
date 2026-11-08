package app.dueffoto

import android.graphics.Bitmap
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Una miniatura della scena sviluppata con [film]: l'anteprima vera, non un colore indicativo. */
@Composable
fun rememberDeveloped(source: Bitmap?, film: Film?): ImageBitmap? {
    val image by produceState<ImageBitmap?>(null, source, film) {
        val src = source ?: return@produceState
        value = withContext(Dispatchers.Default) {
            runCatching { Darkroom.develop(src, film, Develop(grain = false, frame = false)).asImageBitmap() }.getOrNull()
        }
    }
    return image
}

/** Riduce una foto o un fotogramma alla misura delle miniature dei rullini. */
fun thumbSource(src: Bitmap, long: Int = 140): Bitmap {
    val k = long.toFloat() / maxOf(src.width, src.height)
    return Bitmap.createScaledBitmap(src, (src.width * k).toInt().coerceAtLeast(1), (src.height * k).toInt().coerceAtLeast(1), true)
}

/**
 * La scheda di una pellicola: la scena sviluppata con quella pellicola, con sopra il suo rullino
 * (o la scatola delle istantanee, la latta del cinema, il filtro dei Moody).
 */
@Composable
fun FilmCard(film: Film?, selected: Boolean, source: Bitmap?, onClick: () -> Unit, width: Dp = 92.dp) {
    val preview = rememberDeveloped(source, film)
    val lift by animateDpAsState(if (selected) (-6).dp else 0.dp, spring(Spring.DampingRatioMediumBouncy), label = "lift")
    val glow by animateFloatAsState(if (selected) 1f else 0f, label = "glow")
    val accent = film?.let { Color(it.canister.first) } ?: Ink.panel
    Column(Modifier.width(width).press(onClick).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.offset(y = lift).fillMaxWidth().height(width * 1.32f)) {
            Box(
                Modifier
                    .padding(end = width * 0.16f, bottom = width * 0.12f)
                    .fillMaxSize()
                    .shadow(if (selected) 14.dp else 4.dp, RoundedCornerShape(12.dp), ambientColor = Ink.orange, spotColor = Ink.orange.copy(alpha = glow))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.55f), Ink.leather)))
                    .border(if (selected) 2.dp else 1.dp, if (selected) Ink.orange else Ink.panel, RoundedCornerShape(12.dp)),
            ) {
                preview?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                if (film == null) {
                    Text("?", style = Type.title.copy(fontSize = 30.sp, color = Ink.cream.copy(alpha = 0.8f)), modifier = Modifier.align(Alignment.Center))
                }
            }
            if (film != null) {
                Image(
                    painterResource(film.art), null,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.BottomEnd,
                    modifier = Modifier.align(Alignment.BottomEnd).size(width * 0.62f),
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            film?.camera ?: "Scegli dopo", style = Type.name.copy(fontSize = 13.sp, color = if (selected) Ink.cream else Ink.dim),
            maxLines = 1, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis,
        )
        Text(
            (film?.stock ?: "camera oscura").uppercase(),
            style = Type.label.copy(fontSize = 7.5.sp, letterSpacing = 0.6.sp, color = if (selected) Ink.orange else Ink.faint),
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
    }
}

/** La striscia dei rullini sotto il mirino, con gli scaffali e il pulsante del frigo. */
@Composable
fun FilmPicker(kind: Kind, selected: Film?, source: Bitmap?, later: Boolean = true, onPick: (Film?) -> Unit) {
    var shelf by remember(kind) { mutableStateOf<Family?>(null) }
    var fridge by remember { mutableStateOf(false) }
    val films = Films.of(kind).filter { shelf == null || it.family == shelf }
    val list = (if (later && shelf == null) listOf<Film?>(null) else emptyList()) + films
    val state = rememberLazyListState()
    LaunchedEffect(kind, shelf) {
        val i = list.indexOf(selected).coerceAtLeast(0)
        state.scrollToItem((i - 1).coerceAtLeast(0))
    }
    Column {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(Ink.leather).border(1.dp, Ink.panel, RoundedCornerShape(50))
                    .press({ fridge = true }).padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Kitchen, null, tint = Ink.brass, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("FRIGO", style = Type.label.copy(color = Ink.brass, fontWeight = FontWeight.Bold, fontSize = 9.sp))
                }
            }
            if (kind == Kind.ANALOG) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { SmallChip("TUTTE", shelf == null) { shelf = null } }
                    items(Films.analog.map { it.family }.distinct()) { f ->
                        SmallChip(f.label.uppercase(), shelf == f) { shelf = if (shelf == f) null else f }
                    }
                }
            } else {
                Text(
                    "${Films.of(kind).size} LOOK", style = Type.label.copy(fontSize = 9.sp),
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
        LazyRow(
            state = state, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(list, key = { it?.id ?: "later" }) { f ->
                FilmCard(f, f == selected, source, { onPick(f) })
            }
        }
        Text(
            selected?.let { "${it.year} · ${it.note}" } ?: "Scatti al naturale: la pellicola la scegli dopo, in camera oscura.",
            style = Type.body.copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            textAlign = TextAlign.Center,
        )
    }
    if (fridge) {
        Fridge(selected, source, later, onDismiss = { fridge = false }) {
            onPick(it)
            fridge = false
        }
    }
}

@Composable
private fun SmallChip(text: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50))
            .background(if (on) Ink.cream else Color.Transparent)
            .border(1.dp, if (on) Ink.cream else Ink.panel, RoundedCornerShape(50))
            .press(onClick).padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text, style = Type.label.copy(color = if (on) Ink.body else Ink.dim, fontWeight = FontWeight.Bold, fontSize = 9.sp, letterSpacing = 1.sp))
    }
}

/**
 * Il frigo dei rullini: tutte le pellicole sugli scaffali, ognuna con l'anteprima della scena.
 * Scegliere da qui cambia anche il modo (analogico, moody, cinema).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Fridge(selected: Film?, source: Bitmap?, later: Boolean, onDismiss: () -> Unit, onPick: (Film?) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Ink.body,
        dragHandle = { Box(Modifier.padding(top = 10.dp).size(40.dp, 4.dp).clip(RoundedCornerShape(50)).background(Ink.panel)) },
    ) {
        LazyVerticalGrid(
            GridCells.Fixed(3),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(span = { GridItemSpan(3) }) {
                Column(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                    Text("Il frigo dei rullini", style = Type.title)
                    Text(
                        "${Films.all.size} pellicole e look. Ogni anteprima è la tua inquadratura, sviluppata.",
                        style = Type.body.copy(fontSize = 14.sp),
                    )
                }
            }
            if (later) {
                item(span = { GridItemSpan(3) }) { Shelf("Nessuna", "Scatti al naturale e scegli dopo") }
                item { FilmCard(null, selected == null, source, { onPick(null) }, width = 104.dp) }
                item(span = { GridItemSpan(2) }) { Spacer(Modifier.height(1.dp)) }
            }
            Family.entries.forEach { family ->
                val films = Films.all.filter { it.family == family }
                if (films.isEmpty()) return@forEach
                item(span = { GridItemSpan(3) }) { Shelf(family.label, shelfNote(family)) }
                items(films, key = { it.id }) { f ->
                    Box(contentAlignment = Alignment.Center) {
                        FilmCard(f, f == selected, source, { onPick(f) }, width = 104.dp)
                    }
                }
            }
        }
    }
}

private fun shelfNote(family: Family) = when (family) {
    Family.COLOR -> "Negative a colori, dalle compatte alle reflex"
    Family.BW -> "Argento, grana e contrasto"
    Family.SLIDE -> "Diapositive: colori pieni, neri chiusi"
    Family.INSTANT -> "Istantanee con la loro cornice"
    Family.LOMO -> "Plastica, pellicole scadute e incidenti felici"
    Family.MOODY -> "Il look scuro e cinematografico delle foto da telefono"
    Family.CINEMA -> "Color grading da pellicola cinematografica"
}

@Composable
private fun Shelf(title: String, note: String) {
    Column(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = Type.name.copy(fontSize = 20.sp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f).height(1.dp).background(Ink.panel))
        }
        Text(note.uppercase(), style = Type.label.copy(fontSize = 8.sp, color = Ink.faint))
    }
}
