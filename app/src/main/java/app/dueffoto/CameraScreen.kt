package app.dueffoto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaActionSound
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun CameraScreen(studio: Studio) {
    val context = LocalContext.current
    val needed = remember {
        buildList {
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }.toTypedArray()
    }
    fun granted() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    var allowed by remember { mutableStateOf(granted()) }
    var asked by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        allowed = granted()
        asked = true
    }
    LaunchedEffect(Unit) { if (!allowed) launcher.launch(needed) }

    Box(Modifier.fillMaxSize().background(Ink.body)) {
        if (allowed) Viewfinder(studio) else LoadFilm(asked) {
            if (asked) {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            } else launcher.launch(needed)
        }
    }
}

/** Senza permesso: la fotocamera "scarica", da caricare. */
@Composable
private fun LoadFilm(asked: Boolean, onAsk: () -> Unit) {
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Wordmark()
        Spacer(Modifier.height(28.dp))
        Text("La fotocamera è scarica.", style = Type.title.copy(fontSize = 26.sp), textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "Per caricare il primo rullino Dueffoto ha bisogno della fotocamera, e del microfono per i video.",
            style = Type.body, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(Ink.orange).press(onAsk).padding(horizontal = 26.dp, vertical = 14.dp),
        ) {
            Text(if (asked) "APRI LE IMPOSTAZIONI" else "CARICA IL RULLINO", style = Type.label.copy(color = Ink.body, fontWeight = FontWeight.Bold, fontSize = 12.sp))
        }
    }
}

@Composable
private fun Viewfinder(studio: Studio) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val rig = remember { CameraRig(context) }
    val sound = remember { MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) } }
    val shots by studio.roll.shots.collectAsState()

    var frame by remember { mutableStateOf<ImageBitmap?>(null) }
    var busy by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var flash by remember { mutableStateOf(false) }
    var ev by remember { mutableFloatStateOf(0f) }
    var focus by remember { mutableStateOf<Offset?>(null) }
    var focusTick by remember { mutableIntStateOf(0) }
    val blink = remember { Animatable(0f) }

    // Il mirino sviluppa ogni fotogramma con la pellicola corrente (letta dal thread della fotocamera).
    val film = studio.film
    val live = remember { arrayOfNulls<Any>(2) }
    live[0] = film
    live[1] = Develop(strength = studio.strength, ratio = studio.ratio, number = shots.size % 36 + 1)
    var seed by remember { mutableIntStateOf(0) }
    // La scena in piccolo, per le anteprime dei rullini: si aggiorna poco più di una volta al secondo.
    var scene by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var sceneAt by remember { mutableLongStateOf(0L) }
    var tools by remember { mutableStateOf(false) }
    DisposableEffect(rig) {
        rig.onFrame = { bmp ->
            val f = live[0] as Film?
            val o = (live[1] as Develop).copy(seed = seed++)
            val now = System.currentTimeMillis()
            if (now - sceneAt > 900) {
                sceneAt = now
                scene = thumbSource(bmp)
            }
            val out = Darkroom.develop(bmp, f, o)
            bmp.recycle()
            frame = out.asImageBitmap()
        }
        onDispose {
            rig.onFrame = null
            rig.release()
            sound.release()
        }
    }
    LaunchedEffect(studio.front, studio.video) {
        runCatching { rig.start(owner, studio.front, studio.video) }.onFailure { studio.say("Fotocamera non disponibile") }
        rig.setFlash(flash)
        rig.setExposure(ev)
    }

    fun shoot() {
        if (busy) return
        if (studio.video) {
            if (rig.recordingNow) {
                rig.stopRecording()
                return
            }
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            sound.play(MediaActionSound.START_VIDEO_RECORDING)
            recording = true
            elapsed = 0L
            val chosen = studio.film
            val opts = Develop(strength = studio.strength, ratio = studio.ratio)
            rig.record(onTick = { elapsed = it }) { file ->
                recording = false
                sound.play(MediaActionSound.STOP_VIDEO_RECORDING)
                if (file == null) {
                    studio.say("Registrazione non riuscita")
                    return@record
                }
                scope.launch {
                    val shot = studio.roll.addVideo(file, chosen, opts.ratio, opts.strength)
                    if (chosen == null) studio.place = Place.Darkroom(shot.id)
                    else {
                        studio.say("Video nel rullino · sviluppo in corso")
                        studio.printVideo(shot, chosen, studio.opts(shot))
                    }
                }
            }
            return
        }
        busy = true
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        sound.play(MediaActionSound.SHUTTER_CLICK)
        val chosen = studio.film
        val ratio = studio.ratio
        val strength = studio.strength
        scope.launch {
            launch {
                blink.snapTo(1f)
                blink.animateTo(0f, tween(380))
            }
            try {
                val bmp = rig.shoot()
                val shot = studio.roll.addPhoto(bmp, chosen, ratio, strength)
                if (chosen == null) {
                    bmp.recycle()
                    studio.place = Place.Darkroom(shot.id)
                } else {
                    val print = withContext(Dispatchers.Default) { Darkroom.develop(bmp, chosen, studio.opts(shot)) }
                    bmp.recycle()
                    Gallery.savePhoto(context, print, "Dueffoto_${chosen.id}_${shot.id}")
                    print.recycle()
                    val n = studio.roll.shots.value.size
                    studio.say(if (n % 36 == 0) "Rullino finito! Ne carichi subito un altro." else "Stampata · in Galleria")
                }
            } catch (e: Exception) {
                studio.say("Scatto non riuscito")
            } finally {
                busy = false
            }
        }
    }

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        // Barra in alto: marchio, contatore pose, flash, info.
        Row(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Wordmark()
            Spacer(Modifier.weight(1f))
            Lcd(if (recording) timecode(elapsed) else "%02d".format(shots.size % 36 + 1), if (recording) "REC" else "EXP")
            Spacer(Modifier.width(8.dp))
            RoundButton({
                flash = !flash
                rig.setFlash(flash)
            }, size = 40.dp) {
                Icon(if (flash) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff, "Flash", tint = if (flash) Ink.amber else Ink.dim, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(6.dp))
            RoundButton({ studio.info = true }, size = 40.dp) {
                Box {
                    Icon(Icons.Rounded.Info, "Info", tint = Ink.dim, modifier = Modifier.size(20.dp))
                    if (studio.update is UpdateState.Available) {
                        Box(Modifier.size(8.dp).align(Alignment.TopEnd).clip(RoundedCornerShape(50)).background(Ink.orange))
                    }
                }
            }
        }

        // Mirino.
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .aspectRatio(3f / 4f, matchHeightConstraintsFirst = maxWidth / maxHeight > 3f / 4f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF080706))
                    .border(1.dp, Ink.panel, RoundedCornerShape(18.dp))
                    .pointerInputs(
                        onTap = { pos, size ->
                            focus = pos
                            focusTick++
                            rig.focus(pos.x / size.width, pos.y / size.height)
                        },
                        onZoom = { rig.zoomBy(it) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                frame?.let {
                    Image(it, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                } ?: Text("…", style = Type.title.copy(color = Ink.faint))
                if (studio.kind == Kind.CINEMA) CinemaHud(studio, recording, elapsed)
                else if (film?.frame == null || film.frame == Frame.NONE) FilmEdge(film)
                focus?.let { FocusMark(it, focusTick) }
                Box(Modifier.fillMaxSize().alpha(blink.value).background(Color.Black))
                if (studio.update is UpdateState.Available) {
                    UpdatePill(studio, Modifier.align(Alignment.TopCenter).padding(top = 10.dp))
                }
            }
        }

        // Il selettore del modo, con la manopola delle regolazioni accanto.
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Switch(Kind.entries.map { it to it.label }, studio.kind, {
                if (!recording) {
                    studio.kind = it
                    studio.remember()
                }
            }, Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            RoundButton({ tools = !tools }, size = 38.dp) {
                Icon(Icons.Rounded.Tune, "Regolazioni", tint = if (tools || ev != 0f) Ink.orange else Ink.dim, modifier = Modifier.size(18.dp))
            }
        }
        AnimatedVisibility(tools) {
            Column {
                if (studio.kind == Kind.CINEMA) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Ratio.entries.forEach { r ->
                            Chip(r.label, studio.ratio == r) {
                                studio.ratio = r
                                studio.remember()
                            }
                        }
                    }
                }
                Dial(
                    "EV", ev, -2f..2f, (if (ev >= 0) "+" else "") + "%.1f".format(ev),
                    {
                        ev = (it * 3).roundToInt() / 3f
                        rig.setExposure(ev)
                    },
                    Modifier.padding(horizontal = 16.dp),
                    bipolar = true,
                )
                Dial(
                    "DOSE", studio.strength, 0f..1f, "${(studio.strength * 100).roundToInt()}%",
                    {
                        studio.strength = it
                        studio.remember()
                    },
                    Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        FilmPicker(studio.kind, film, scene) { studio.choose(it) }

        // Foto o video, come sulle fotocamere: due parole sopra lo scatto.
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.Center) {
            listOf(false to "FOTO", true to "VIDEO").forEach { (v, label) ->
                val on = studio.video == v
                Column(
                    Modifier.press({ if (!recording) studio.video = v }).padding(horizontal = 14.dp, vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(label, style = Type.label.copy(color = if (on) Ink.orange else Ink.faint, fontWeight = FontWeight.Bold, letterSpacing = 3.sp))
                    Box(Modifier.padding(top = 3.dp).size(4.dp).clip(RoundedCornerShape(50)).background(if (on) Ink.orange else Color.Transparent))
                }
            }
        }

        // Comandi: rullino, scatto, fotocamera frontale.
        Row(
            Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 2.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(Ink.leather)
                    .border(1.dp, Ink.panel, RoundedCornerShape(14.dp)).press({ studio.place = Place.Roll }),
                contentAlignment = Alignment.Center,
            ) {
                shots.firstOrNull()?.let { ShotThumb(studio, it, Modifier.fillMaxSize()) }
                    ?: Icon(Icons.Rounded.PhotoLibrary, "Rullino", tint = Ink.dim)
            }
            ShutterButton(studio.video, recording, enabled = !busy, onClick = ::shoot)
            RoundButton({ if (!recording) studio.front = !studio.front }, size = 56.dp) {
                Icon(Icons.Rounded.Cameraswitch, "Gira", tint = Ink.cream)
            }
        }
        LabBar(studio)
    }
}

/** HUD da cinepresa: REC, timecode, fps e rapporto. */
@Composable
private fun CinemaHud(studio: Studio, recording: Boolean, elapsed: Long) {
    Box(Modifier.fillMaxSize().padding(14.dp)) {
        Row(Modifier.align(Alignment.TopStart), verticalAlignment = Alignment.CenterVertically) {
            if (recording) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(Ink.rec))
                Spacer(Modifier.width(6.dp))
            }
            Text(if (recording) "REC  ${timecode(elapsed)}" else "STBY", style = Type.label.copy(color = if (recording) Ink.rec else Ink.cream, fontWeight = FontWeight.Bold))
        }
        Text(
            "${studio.ratio.label}  ·  ${if (studio.video) "30 FPS" else "STILL"}",
            style = Type.label.copy(color = Ink.cream.copy(alpha = 0.8f)), modifier = Modifier.align(Alignment.TopEnd),
        )
        Text(
            (studio.film?.camera ?: "LOG").uppercase(),
            style = Type.label.copy(color = Ink.amber, fontWeight = FontWeight.Bold), modifier = Modifier.align(Alignment.BottomStart),
        )
        Text(
            studio.film?.stock?.uppercase() ?: "SCEGLI DOPO",
            style = Type.label.copy(color = Ink.cream.copy(alpha = 0.7f), fontSize = 8.sp), modifier = Modifier.align(Alignment.BottomEnd),
        )
        // Angoli del quadro.
        Canvas(Modifier.fillMaxSize()) {
            val l = 18.dp.toPx()
            val s = Stroke(1.5f)
            val c = Ink.cream.copy(alpha = 0.6f)
            val inset = 20.dp.toPx()
            listOf(
                Offset(0f, inset) to Offset(1f, 1f), Offset(size.width, inset) to Offset(-1f, 1f),
                Offset(0f, size.height - inset) to Offset(1f, -1f), Offset(size.width, size.height - inset) to Offset(-1f, -1f),
            ).forEach { (p, d) ->
                drawLine(c, p, p + Offset(l * d.x, 0f), s.width)
                drawLine(c, p, p + Offset(0f, l * d.y), s.width)
            }
            drawCircle(c, 3f, center)
        }
    }
}

/** Le scritte arancioni sul bordo pellicola, nel mirino. */
@Composable
private fun FilmEdge(film: Film?) {
    Box(Modifier.fillMaxSize().padding(12.dp)) {
        Text(
            film?.let {
                if (it.kind == Kind.MOODY) "${it.camera.uppercase()}  ▸  ${it.year.uppercase()}"
                else "${it.stock.uppercase()}  ▸  ISO ${it.iso}"
            } ?: "NEGATIVO  ▸  SCEGLI DOPO",
            style = Type.label.copy(color = Ink.orange, fontWeight = FontWeight.Bold, fontSize = 9.sp),
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

@Composable
private fun FocusMark(pos: Offset, tick: Int) {
    val a = remember(tick) { Animatable(1f) }
    LaunchedEffect(tick) { a.animateTo(0f, tween(900)) }
    Canvas(Modifier.fillMaxSize().alpha(a.value)) {
        val s = 64.dp.toPx() * (1f + 0.25f * a.value)
        drawRect(Ink.brass, Offset(pos.x - s / 2, pos.y - s / 2), Size(s, s), style = Stroke(2f))
    }
}

@Composable
private fun UpdatePill(studio: Studio, modifier: Modifier) {
    val release = (studio.update as? UpdateState.Available)?.release ?: return
    Box(
        modifier.clip(RoundedCornerShape(50)).background(Ink.orange).press({ studio.info = true })
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text("NUOVA VERSIONE ${release.version} · AGGIORNA", style = Type.label.copy(color = Ink.body, fontWeight = FontWeight.Bold))
    }
}

/** Barra di sviluppo dei video in corso. */
@Composable
fun LabBar(studio: Studio) {
    AnimatedVisibility(studio.lab != null, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
        val p = studio.lab ?: 1f
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
            Text("SVILUPPO VIDEO  ${(p * 100).roundToInt()}%", style = Type.label.copy(color = Ink.amber))
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).background(Ink.panel)) {
                Box(Modifier.fillMaxWidth(p).height(4.dp).background(Ink.orange))
            }
        }
    }
}

@Composable
fun Toast(text: String?) {
    Box(Modifier.fillMaxSize().systemBarsPadding().padding(bottom = 140.dp), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(text != null, enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut()) {
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(Ink.cream).padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Text(text ?: "", style = Type.label.copy(color = Ink.body, fontWeight = FontWeight.Bold, fontSize = 11.sp))
            }
        }
    }
}

fun timecode(ms: Long): String {
    val s = ms / 1000
    val f = (ms % 1000) * 24 / 1000
    return "%02d:%02d:%02d".format(s / 60, s % 60, f)
}

private fun Modifier.pointerInputs(onTap: (Offset, Size) -> Unit, onZoom: (Float) -> Unit): Modifier =
    this
        .pointerInput("tap") { detectTapGestures { onTap(it, Size(size.width.toFloat(), size.height.toFloat())) } }
        .pointerInput("zoom") { detectTransformGestures { _, _, zoom, _ -> if (zoom != 1f) onZoom(zoom) } }
