package no.jodal.salat.apps.rise

import kotlin.time.Duration.Companion.hours
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

/** Én alarm eller ett varsel iOS skal planlegge. Sendes til Swift som JSON. */
@Serializable
data class PlannedItem(
    /** Stabil ID, så Swift kan se hva som allerede er planlagt. */
    val id: String,
    val kind: AlarmKind,
    val fireAtMillis: Long,
    val title: String,
    val body: String,
    val sound: String,
    val snoozeMinutes: Int,
)

object RisePlanner {
    /** Hvor langt fram i tid Rise planlegger. Resten fylles på når appen åpnes. */
    const val WindowDays = 21

    /** iOS tillater 64 planlagte varsler per app. Noen er holdt av til resten av Salat. */
    const val MaxNotifications = 56

    /** AlarmKit har også en grense, men Apple oppgir ikke hvor høy den er. */
    const val MaxAlarms = 40

    fun plan(data: RiseData, now: LocalDateTime, timeZone: TimeZone): List<PlannedItem> {
        val until = now.date.plus(DatePeriod(days = WindowDays))
        val nowMillis = now.toInstant(timeZone).toEpochMilliseconds()
        // Begynn litt tilbake i tid, så oppfølgingsalarmer etter en alarm som nettopp har ringt kommer med.
        val since = (now.toInstant(timeZone) - 2.hours).toLocalDateTime(timeZone)
        val items = data.alarms
            .filter { it.enabled }
            .flatMap { alarm ->
                alarm.occurrences(since, until).flatMap { itemsFor(alarm, it, data.confirmedAwake, timeZone) }
            }
            .filter { it.fireAtMillis > nowMillis }
            .sortedBy { it.fireAtMillis }
        return items.filter { it.kind == AlarmKind.Alarm }.take(MaxAlarms) +
            items.filter { it.kind == AlarmKind.Notification }.take(MaxNotifications)
    }

    private fun itemsFor(
        alarm: RiseAlarm,
        at: LocalDateTime,
        confirmedAwake: Set<String>,
        timeZone: TimeZone,
    ): List<PlannedItem> {
        val fireAt = at.toInstant(timeZone).toEpochMilliseconds()
        val title = alarm.title.ifBlank { if (alarm.kind == AlarmKind.Alarm) "Alarm" else "Påminnelse" }
        val main = PlannedItem(
            id = occurrenceKey(alarm, at),
            kind = alarm.kind,
            fireAtMillis = fireAt,
            title = title,
            body = "Kl. ${formatTime(alarm.time)}",
            sound = alarm.sound,
            snoozeMinutes = if (alarm.kind == AlarmKind.Alarm) alarm.snoozeMinutes else 0,
        )
        return when (alarm.kind) {
            AlarmKind.Alarm -> listOf(main) + awakeCheckFollowUps(alarm, at, main, confirmedAwake)
            AlarmKind.Notification -> earlyWarning(alarm, main) + main
        }
    }

    private fun awakeCheckFollowUps(
        alarm: RiseAlarm,
        at: LocalDateTime,
        main: PlannedItem,
        confirmedAwake: Set<String>,
    ): List<PlannedItem> {
        if (!alarm.awakeCheck || occurrenceKey(alarm, at) in confirmedAwake) return emptyList()
        return (0 until AwakeCheck.FollowUps).map { index ->
            main.copy(
                id = "${main.id}#oppe${index + 1}",
                fireAtMillis = main.fireAtMillis + AwakeCheck.followUpDelayMinutes(alarm, index) * 60_000L,
                title = "Er du oppe?",
                body = "Trykk «Jeg er oppe» i Rise",
                sound = RiseSound.Loud.id,
                snoozeMinutes = 0,
            )
        }
    }

    private fun earlyWarning(alarm: RiseAlarm, main: PlannedItem): List<PlannedItem> {
        if (alarm.earlyWarningMinutes <= 0) return emptyList()
        return listOf(
            main.copy(
                id = "${main.id}#forvarsel",
                fireAtMillis = main.fireAtMillis - alarm.earlyWarningMinutes * 60_000L,
                body = "Om ${formatMinutes(alarm.earlyWarningMinutes)}, kl. ${formatTime(alarm.time)}",
            ),
        )
    }
}
