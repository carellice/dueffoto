package app.dueffoto

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Miniatura di uno scatto, sviluppata con la sua pellicola. */
@Composable
fun ShotThumb(studio: Studio, shot: Shot, modifier: Modifier = Modifier) {
    val image by produceState<ImageBitmap?>(null, shot) {
        value = withContext(Dispatchers.Default) {
            studio.roll.original(shot, 360)?.let { base ->
                Darkroom.develop(base, shot.film, studio.opts(shot).copy(frame = false)).asImageBitmap()
            }
        }
    }
    Box(modifier.background(Ink.leather), contentAlignment = Alignment.Center) {
        image?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        if (shot.video) {
            Icon(Icons.Rounded.PlayArrow, null, tint = Ink.cream, modifier = Modifier.size(22.dp).align(Alignment.BottomStart).padding(2.dp))
        }
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        RoundButton(onBack, size = 42.dp) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Indietro", tint = Ink.cream) }
        Spacer(Modifier.width(14.dp))
        Text(title, style = Type.title.copy(fontSize = 26.sp), modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
fun RollScreen(studio: Studio) {
    val shots by studio.roll.shots.collectAsState()
    Column(Modifier.fillMaxSize().background(Ink.body).systemBarsPadding()) {
        TopBar("Rullino", { studio.place = Place.Camera }) {
            Text("${shots.size} POSE", style = Type.label, modifier = Modifier.padding(end = 10.dp))
        }
        FilmStripDivider()
        if (shots.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(40.dp),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Rullino vergine.", style = Type.title, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Gli originali dei tuoi scatti restano qui: puoi svilupparli di nuovo con un'altra pellicola quando vuoi.", style = Type.body, textAlign = TextAlign.Center)
            }
        } else {
            LazyVerticalGrid(
                GridCells.Fixed(3),
                contentPadding = PaddingValues(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(shots, key = { it.id }) { shot ->
                    Column(Modifier.press({ studio.place = Place.Darkroom(shot.id) })) {
                        ShotThumb(studio, shot, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)))
                        Text(
                            (shot.film?.stock ?: "da sviluppare").uppercase(), style = Type.label.copy(fontSize = 8.sp, color = if (shot.film == null) Ink.orange else Ink.faint),
                            maxLines = 1, modifier = Modifier.padding(top = 3.dp, start = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DarkroomScreen(studio: Studio, shotId: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shots by studio.roll.shots.collectAsState()
    val shot = shots.firstOrNull { it.id == shotId } ?: run {
        LaunchedEffect(Unit) { studio.place = Place.Roll }
        return
    }
    var kind by remember { mutableStateOf(shot.film?.kind ?: studio.kind) }
    var film by remember { mutableStateOf(shot.film ?: if (kind == Kind.ANALOG) Films.analog.first() else Films.cinema.first()) }
    var strength by remember { mutableStateOf(shot.strength) }
    var ratio by remember { mutableStateOf(shot.ratio) }
    var grain by remember { mutableStateOf(true) }
    var frame by remember { mutableStateOf(true) }
    var printing by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf<Uri?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val base by produceState<Bitmap?>(null, shot.id) { value = studio.roll.original(shot, 1400) }
    val opts = Develop(strength, ratio, (shot.id % 997).toInt(), grain = grain && !shot.video, frame = frame && !shot.video)
    val preview by produceState<ImageBitmap?>(null, base, film, opts) {
        val b = base ?: return@produceState
        delay(40)
        value = withContext(Dispatchers.Default) { Darkroom.develop(b, film, opts).asImageBitmap() }
    }

    Column(Modifier.fillMaxSize().background(Ink.body).systemBarsPadding()) {
        TopBar("Camera oscura", { studio.place = Place.Roll }) {
            RoundButton({ confirmDelete = true }, size = 42.dp) { Icon(Icons.Rounded.Delete, "Elimina", tint = Ink.dim) }
            saved?.takeIf { it.scheme == "content" }?.let { uri ->
                Spacer(Modifier.width(8.dp))
                RoundButton({
                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).setType(if (shot.video) "video/mp4" else "image/jpeg")
                                .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                            "Condividi",
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }, size = 42.dp) { Icon(Icons.Rounded.Share, "Condividi", tint = Ink.cream) }
            }
        }
        Box(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(18.dp)).background(Color(0xFF080706)),
            contentAlignment = Alignment.Center,
        ) {
            preview?.let { Image(it, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(10.dp)) }
                ?: CircularProgressIndicator(color = Ink.orange)
            if (shot.video) {
                Text(
                    "VIDEO · ANTEPRIMA DEL PRIMO FOTOGRAMMA", style = Type.label.copy(color = Ink.amber, fontSize = 9.sp),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            Switch(listOf(Kind.ANALOG to "ANALOGICO", Kind.CINEMA to "CINEMA"), kind, {
                kind = it
                film = if (it == Kind.ANALOG) Films.analog.first() else Films.cinema.first()
            })
            Spacer(Modifier.weight(1f))
            if (!shot.video) {
                Chip("GRANA", grain) { grain = !grain }
                Chip("CORNICE", frame) { frame = !frame }
            }
        }
        if (kind == Kind.CINEMA) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Ratio.entries.forEach { r -> Chip(r.label, ratio == r) { ratio = r } }
            }
        }
        Dial("DOSE", strength, 0f..1f, "${(strength * 100).roundToInt()}%", { strength = it }, Modifier.padding(horizontal = 16.dp))
        FilmPickerNoLater(kind, film) { film = it }

        val developing = shot.video && studio.lab != null
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp).navigationBarsPadding()
                .clip(RoundedCornerShape(50)).background(if (printing || developing) Ink.panel else Ink.orange)
                .press({
                    studio.roll.save(shot.copy(filmId = film.id, ratio = ratio, strength = strength))
                    if (shot.video) {
                        studio.printVideo(shot, film, opts) { saved = it }
                    } else {
                        printing = true
                        scope.launch {
                            saved = runCatching { studio.printPhoto(shot, film, opts) }.getOrNull()
                            printing = false
                            studio.say(if (saved != null) "Stampata · in Galleria" else "Stampa non riuscita")
                        }
                    }
                }, enabled = !printing && !developing)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                when {
                    printing -> "STAMPA IN CORSO…"
                    developing -> "SVILUPPO ${((studio.lab ?: 0f) * 100).roundToInt()}%"
                    shot.video -> "SVILUPPA IL VIDEO"
                    else -> "STAMPA"
                },
                style = Type.label.copy(color = if (printing || developing) Ink.amber else Ink.body, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 3.sp),
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Ink.leather,
            title = { Text("Buttare il negativo?", style = Type.title.copy(fontSize = 22.sp)) },
            text = { Text("L'originale sparisce dal rullino. Le stampe già salvate in Galleria restano.", style = Type.body) },
            confirmButton = {
                TextButton({
                    confirmDelete = false
                    studio.roll.delete(shot)
                    studio.place = Place.Roll
                }) { Text("ELIMINA", style = Type.label.copy(color = Ink.rec, fontWeight = FontWeight.Bold)) }
            },
            dismissButton = { TextButton({ confirmDelete = false }) { Text("ANNULLA", style = Type.label.copy(color = Ink.cream)) } },
        )
    }
}

/** In camera oscura si sceglie sempre una pellicola: niente "Scegli dopo". */
@Composable
private fun FilmPickerNoLater(kind: Kind, selected: Film, onPick: (Film) -> Unit) {
    FilmPicker(kind, selected, later = false) { it?.let(onPick) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoSheet(studio: Studio) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = { studio.info = false }, containerColor = Ink.leather) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
            Wordmark()
            Spacer(Modifier.height(4.dp))
            Text("Fotocamere che non esistono più, nel tuo telefono.", style = Type.body)
            Spacer(Modifier.height(18.dp))
            Text(
                "${Films.analog.size} fotocamere analogiche · ${Films.cinema.size} look cinema\n" +
                    "Scegli la pellicola prima di scattare, oppure scatta e sviluppa dopo in camera oscura: gli originali restano nel rullino.",
                style = Type.label.copy(color = Ink.dim, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
            )
            Spacer(Modifier.height(20.dp))
            FilmStripDivider()
            Spacer(Modifier.height(16.dp))
            val state = studio.update
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("VERSIONE", style = Type.label)
                    Text(Updater.currentVersion(context), style = Type.name.copy(fontSize = 20.sp))
                }
                val (label, action) = when (state) {
                    is UpdateState.Available -> "AGGIORNA A ${state.release.version}" to { studio.downloadUpdate() }
                    is UpdateState.Downloading -> "DOWNLOAD ${(state.progress * 100).roundToInt()}%" to {}
                    is UpdateState.Ready -> "INSTALLA" to { Updater.install(context, state.file) }
                    UpdateState.Checking -> "CONTROLLO…" to {}
                    UpdateState.UpToDate -> "AGGIORNATA ✓" to { studio.checkUpdate(silent = false) }
                    UpdateState.Idle -> "CONTROLLA AGGIORNAMENTI" to { studio.checkUpdate(silent = false) }
                }
                val hot = state is UpdateState.Available || state is UpdateState.Ready
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(if (hot) Ink.orange else Color.Transparent)
                        .border(1.dp, if (hot) Ink.orange else Ink.panel, RoundedCornerShape(50))
                        .press(action).padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(label, style = Type.label.copy(color = if (hot) Ink.body else Ink.cream, fontWeight = FontWeight.Bold))
                }
            }
            (state as? UpdateState.Available)?.release?.notes?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, style = Type.body.copy(fontSize = 13.sp), maxLines = 6)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "github.com/carellice/dueffoto", style = Type.label.copy(color = Ink.orange),
                modifier = Modifier.press({
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/carellice/dueffoto")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }),
            )
            Spacer(Modifier.height(6.dp))
            Text("Una app Dueffe · ${SimpleDateFormat("yyyy", Locale.ITALY).format(Date())}", style = Type.label.copy(color = Ink.faint))
        }
    }
}
