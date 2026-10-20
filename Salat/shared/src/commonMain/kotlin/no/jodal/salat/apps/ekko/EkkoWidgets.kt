package no.jodal.salat.apps.ekko

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.jodal.salat.NativeBridge
import no.jodal.salat.apps.ekko.ui.EkkoPalette
import no.jodal.salat.apps.ekko.ui.resample

/** Det widgetene til Ekko trenger. Swift leser det som `EkkoWidgetData`. */
@Serializable
data class EkkoWidgetData(
    val sounds: List<EkkoWidgetSound>,
    /** Lydene på lydbrettet. Er det tomt, viser widgeten de nyeste lydene. */
    val soundboard: List<String>,
)

@Serializable
data class EkkoWidgetSound(
    val id: String,
    val title: String,
    val artist: String,
    /** De to fargene i omslaget, som `#RRGGBB`. */
    val colors: List<String>,
    /** En kort lydbølge for omslaget. */
    val waveform: List<Float>,
)

private val widgetJsonFormat = Json { encodeDefaults = true }

internal fun widgetJson(store: EkkoStore): String {
    val data = EkkoWidgetData(
        sounds = store.library.map { clip ->
            val (from, to) = EkkoPalette.cover(clip.id)
            EkkoWidgetSound(
                id = clip.id,
                title = clip.title,
                artist = clip.artist,
                colors = listOf(from.hex(), to.hex()),
                waveform = resample(clip.trimmedWaveform(), 7).map { (it * 100).toInt() / 100f },
            )
        },
        soundboard = store.data.soundboard,
    )
    return widgetJsonFormat.encodeToString(EkkoWidgetData.serializer(), data)
}

private fun Color.hex(): String = "#" + (toArgb() and 0xFFFFFF).toString(16).padStart(6, '0').uppercase()

/** Kalles fra Swift når appen blir aktiv, så widgetene har lydene selv om Ekko ikke er åpnet. */
fun refreshEkkoWidget(bridge: NativeBridge) = EkkoStore.get(bridge).publishWidget()

/** Spiller en lyd fra lydbrettet i widgeten. Kalles fra Swift, også når appen ikke er åpen. */
fun playFromWidget(bridge: NativeBridge, clipId: String) {
    val store = EkkoStore.get(bridge)
    val clip = store.clip(clipId) ?: return
    // Bare den ene lyden, så lydbrettet ikke fortsetter med neste lyd.
    EkkoPlayer.get(store, bridge).play(listOf(clip), EkkoPlayer.QueueSource(WidgetSourceId, "Lydbrettet"), startAt = clip)
}

private const val WidgetSourceId = "widget"
