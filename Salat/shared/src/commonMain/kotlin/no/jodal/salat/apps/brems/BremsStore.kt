package no.jodal.salat.apps.brems

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.time.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import no.jodal.salat.NativeBridge

/** Holder på det Brems husker, og lagrer det på telefonen. Brukes både av skjermene og av Snarveier. */
class BremsStore private constructor(private val bridge: NativeBridge) {
    var data by mutableStateOf(load())
        private set

    fun update(transform: (BremsData) -> BremsData) {
        data = transform(data)
        bridge.saveText(StorageKey, json.encodeToString(BremsData.serializer(), data))
    }

    fun updateSettings(transform: (BremsSettings) -> BremsSettings) = update { it.copy(settings = transform(it.settings)) }

    /** En app er åpnet. Gir `true` hvis Brems skal vise en pause. */
    fun opened(app: String?): Boolean {
        val now = nowMillis()
        val before = data.active
        val (next, decision) = BremsRules.opened(BremsRules.trim(data, now), app, now)
        update { next }
        if (next.active != before) scheduleNotices()
        return decision is Decision.Wait
    }

    fun closed() {
        val before = data.active
        update { BremsRules.closed(it, nowMillis()) }
        if (data.active != before) scheduleNotices()
    }

    fun currentPause(): Pause? = BremsRules.currentPause(data, nowMillis())

    /** Går tilbake til appen etter pausen. Kjenner vi appen, åpnes den. */
    fun continueToApp() {
        val app = data.pauses.lastOrNull()?.app.orEmpty()
        update { BremsRules.continueToApp(it, nowMillis()) }
        appUrl(app)?.let(bridge::openUrl)
    }

    fun stop() = update(BremsRules::stop)

    fun answerWhy(why: String?) = update { BremsRules.answerWhy(it, why) }

    fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

    /** Planlegger varslene for økta som pågår, eller fjerner dem hvis ingen app er åpen. */
    private fun scheduleNotices() {
        val active = data.active
        val notices = if (active == null || !data.settings.enabled) {
            emptyList()
        } else {
            BremsRules.notifications(data.settings, active.app).map {
                ScheduledNotice(it.title, it.body, active.startMillis + it.minutes * 60_000L)
            }
        }
        bridge.bremsNotify(json.encodeToString(ListSerializer(ScheduledNotice.serializer()), notices))
    }

    private fun load(): BremsData = bridge.loadText(StorageKey)
        ?.let { runCatching { json.decodeFromString(BremsData.serializer(), it) }.getOrNull() }
        ?: BremsData()

    companion object {
        private const val StorageKey = "brems.data"
        private val json = Json { ignoreUnknownKeys = true }
        private var instance: BremsStore? = null

        fun get(bridge: NativeBridge): BremsStore = instance ?: BremsStore(bridge).also { instance = it }
    }
}

/** Et varsel som skal komme ved [atMillis]. */
@Serializable
data class ScheduledNotice(val title: String, val body: String, val atMillis: Long)

/** Kalles fra Snarveier når en app åpnes. Gir `true` hvis Brems skal åpnes og vise en pause. */
fun bremsOpened(bridge: NativeBridge, app: String?): Boolean = BremsStore.get(bridge).opened(app)

/** Kalles fra Snarveier når en app lukkes. */
fun bremsClosed(bridge: NativeBridge) = BremsStore.get(bridge).closed()

/** Lenken som åpner [app], for appene vi kjenner. */
fun appUrl(app: String): String? = AppSchemes[app.trim().lowercase()]

private val AppSchemes = mapOf(
    "snapchat" to "snapchat://",
    "youtube" to "youtube://",
    "tiktok" to "tiktok://",
    "instagram" to "instagram://",
    "facebook" to "fb://",
    "messenger" to "fb-messenger://",
    "x" to "twitter://",
    "twitter" to "twitter://",
    "reddit" to "reddit://",
    "threads" to "barcelona://",
    "pinterest" to "pinterest://",
    "discord" to "discord://",
    "twitch" to "twitch://",
    "netflix" to "nflx://",
    "spotify" to "spotify://",
    "linkedin" to "linkedin://",
)
