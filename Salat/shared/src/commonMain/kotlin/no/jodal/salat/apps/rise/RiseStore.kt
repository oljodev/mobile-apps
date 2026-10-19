package no.jodal.salat.apps.rise

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.time.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import no.jodal.salat.NativeBridge

/** Holder på alarmene i Rise, lagrer dem og sørger for at iOS har planlagt dem. */
class RiseStore private constructor(private val bridge: NativeBridge) {
    var data by mutableStateOf(load())
        private set

    /** Feilmelding fra siste gang alarmene ble planlagt, eller `null`. */
    var scheduleError by mutableStateOf<String?>(null)
        private set

    fun update(transform: (RiseData) -> RiseData) {
        val today = now().date
        data = transform(data).let { updated ->
            // Hoppede dager som har vært, trengs ikke lenger.
            updated.copy(alarms = updated.alarms.map { a -> a.copy(skippedDates = a.skippedDates.filter { it >= today }.toSet()) })
        }
        bridge.saveText(StorageKey, json.encodeToString(RiseData.serializer(), data))
        reschedule()
    }

    /** Kalles når appen åpnes, for å fylle på alarmer. */
    fun refresh() = reschedule()

    fun now(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    private fun reschedule() {
        val plan = RisePlan(
            items = RisePlanner.plan(data, now(), TimeZone.currentSystemDefault()),
            activeAlarmIds = data.alarms.filter { it.enabled }.map { it.id },
        )
        bridge.applyRiseSchedule(json.encodeToString(RisePlan.serializer(), plan)) { error -> scheduleError = error }
    }

    private fun load(): RiseData = bridge.loadText(StorageKey)
        ?.let { runCatching { json.decodeFromString(RiseData.serializer(), it) }.getOrNull() }
        ?: RiseData()

    companion object {
        private const val StorageKey = "rise.data"
        private val json = Json { ignoreUnknownKeys = true }
        private var instance: RiseStore? = null

        fun get(bridge: NativeBridge): RiseStore = instance ?: RiseStore(bridge).also { instance = it }
    }
}

/** Kalles fra Swift hver gang appen åpnes. */
fun refreshRise(bridge: NativeBridge) = RiseStore.get(bridge).refresh()
