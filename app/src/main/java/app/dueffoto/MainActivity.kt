package app.dueffoto

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Dove siamo: fotocamera, rullino o camera oscura su uno scatto. */
sealed interface Place {
    data object Camera : Place
    data object Roll : Place
    data class Darkroom(val shotId: Long) : Place
}

/** Lo stato dell'app: pellicola scelta, modo, rullino, aggiornamenti e sviluppi in corso. */
class Studio(private val context: Context, private val scope: CoroutineScope) {
    private val prefs = context.getSharedPreferences("dueffoto", Context.MODE_PRIVATE)
    val roll = Roll(context)

    var place by mutableStateOf<Place>(Place.Camera)
    var kind by mutableStateOf(runCatching { Kind.valueOf(prefs.getString("kind", "")!!) }.getOrDefault(Kind.ANALOG))
    var analogFilm by mutableStateOf(pick(prefs.getString("analog", "trip35"), Films.analog.first()))
    var cinemaFilm by mutableStateOf(pick(prefs.getString("cinema", "golden"), Films.cinema.first()))
    var moodyFilm by mutableStateOf(pick(prefs.getString("moody", "dusk"), Films.moody.first()))
    var ratio by mutableStateOf(runCatching { Ratio.valueOf(prefs.getString("ratio", "")!!) }.getOrDefault(Ratio.HD))
    var strength by mutableStateOf(prefs.getFloat("strength", 1f))
    var video by mutableStateOf(false)
    var front by mutableStateOf(false)
    var update by mutableStateOf<UpdateState>(UpdateState.Idle)
    var info by mutableStateOf(false)
    /** Avanzamento dello sviluppo video in corso, null se fermo. */
    var lab by mutableStateOf<Float?>(null)
    var toast by mutableStateOf<String?>(null)

    val film: Film?
        get() = when (kind) {
            Kind.ANALOG -> analogFilm
            Kind.MOODY -> moodyFilm
            Kind.CINEMA -> cinemaFilm
        }

    /** "Scegli dopo" è salvato come stringa vuota. */
    private fun pick(id: String?, default: Film): Film? = if (id == "") null else Films.byId(id) ?: default

    fun remember() {
        prefs.edit()
            .putString("kind", kind.name)
            .putString("analog", analogFilm?.id ?: "")
            .putString("cinema", cinemaFilm?.id ?: "")
            .putString("moody", moodyFilm?.id ?: "")
            .putString("ratio", ratio.name)
            .putFloat("strength", strength)
            .apply()
    }

    /** Sceglie la pellicola; se viene da un altro scaffale del frigo, cambia anche il modo. */
    fun choose(film: Film?) {
        if (film != null) kind = film.kind
        when (kind) {
            Kind.ANALOG -> analogFilm = film
            Kind.MOODY -> moodyFilm = film
            Kind.CINEMA -> cinemaFilm = film
        }
        remember()
    }

    fun say(text: String) {
        toast = text
        scope.launch {
            delay(2600)
            if (toast == text) toast = null
        }
    }

    fun opts(shot: Shot) = Develop(strength = shot.strength, ratio = shot.ratio, seed = (shot.id % 997).toInt())

    /** Sviluppa la foto alla massima qualità e la mette in galleria. */
    suspend fun printPhoto(shot: Shot, film: Film, opts: Develop): Uri? {
        val base = roll.original(shot, Darkroom.MAX_SIDE) ?: return null
        val print = withContext(Dispatchers.Default) { Darkroom.develop(base, film, opts) }
        return Gallery.savePhoto(context, print, "Dueffoto_${film.id}_${shot.id}").also { print.recycle() }
    }

    /** Sviluppa un video in sottofondo e lo mette in galleria. */
    fun printVideo(shot: Shot, film: Film, opts: Develop, onDone: (Uri?) -> Unit = {}) {
        if (lab != null) {
            say("C'è già un video in sviluppo")
            return
        }
        lab = 0f
        scope.launch {
            val uri = runCatching {
                val tmp = VideoLab.develop(context, roll.file(shot), film, opts) { lab = it }
                Gallery.saveVideo(context, tmp, "Dueffoto_${film.id}_${shot.id}").also { tmp.delete() }
            }.getOrNull()
            lab = null
            say(if (uri != null) "Video sviluppato · in Galleria" else "Sviluppo del video non riuscito")
            onDone(uri)
        }
    }

    fun checkUpdate(silent: Boolean) {
        if (update is UpdateState.Checking || update is UpdateState.Downloading) return
        update = UpdateState.Checking
        scope.launch {
            update = runCatching { Updater.check(context) }.getOrNull()?.let { UpdateState.Available(it) }
                ?: if (silent) UpdateState.Idle else UpdateState.UpToDate
        }
    }

    fun downloadUpdate() {
        val release = (update as? UpdateState.Available)?.release ?: return
        if (!Updater.canInstall(context)) {
            Updater.openInstallPermission(context)
            return
        }
        update = UpdateState.Downloading(release, 0f)
        scope.launch {
            update = runCatching {
                val file = Updater.download(context, release) { p -> update = UpdateState.Downloading(release, p) }
                UpdateState.Ready(release, file)
            }.getOrElse {
                say("Download non riuscito")
                UpdateState.Available(release)
            }
            (update as? UpdateState.Ready)?.let { Updater.install(context, it.file) }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        ResourcesCompat.getFont(this, R.font.mono_bold)?.let { Darkroom.mono = it }
        val studio = Studio(applicationContext, lifecycleScope)
        studio.checkUpdate(silent = true)
        setContent {
            DueffotoTheme {
                BackHandler(studio.place != Place.Camera) {
                    studio.place = when (studio.place) {
                        is Place.Darkroom -> Place.Roll
                        else -> Place.Camera
                    }
                }
                AnimatedContent(
                    studio.place,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                    contentKey = { it::class },
                    label = "place",
                ) { place ->
                    when (place) {
                        Place.Camera -> CameraScreen(studio)
                        Place.Roll -> RollScreen(studio)
                        is Place.Darkroom -> DarkroomScreen(studio, place.shotId)
                    }
                }
                if (studio.info) InfoSheet(studio)
                Toast(studio.toast)
            }
        }
    }
}
