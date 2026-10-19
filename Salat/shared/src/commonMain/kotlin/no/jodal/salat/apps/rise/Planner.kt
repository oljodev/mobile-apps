package no.jodal.salat.apps.rise

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
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
    val awakeCheck: Boolean,
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
        val items = data.alarms
            .filter { it.enabled }
            .flatMap { alarm -> alarm.occurrences(now, until).flatMap { itemsFor(alarm, it, now, timeZone) } }
            .sortedBy { it.fireAtMillis }
        return items.filter { it.kind == AlarmKind.Alarm }.take(MaxAlarms) +
            items.filter { it.kind == AlarmKind.Notification }.take(MaxNotifications)
    }

    private fun itemsFor(
        alarm: RiseAlarm,
        at: LocalDateTime,
        now: LocalDateTime,
        timeZone: TimeZone,
    ): List<PlannedItem> {
        val fireAt = at.toInstant(timeZone).toEpochMilliseconds()
        val title = alarm.title.ifBlank { if (alarm.kind == AlarmKind.Alarm) "Alarm" else "Påminnelse" }
        val main = PlannedItem(
            id = "${alarm.id}@$at",
            kind = alarm.kind,
            fireAtMillis = fireAt,
            title = title,
            body = "Kl. ${formatTime(alarm.time)}",
            sound = alarm.sound,
            snoozeMinutes = if (alarm.kind == AlarmKind.Alarm) alarm.snoozeMinutes else 0,
            awakeCheck = alarm.kind == AlarmKind.Alarm && alarm.awakeCheck,
        )
        if (alarm.kind != AlarmKind.Notification || alarm.earlyWarningMinutes <= 0) return listOf(main)

        val earlyAt = fireAt - alarm.earlyWarningMinutes * 60_000L
        if (earlyAt <= now.toInstant(timeZone).toEpochMilliseconds()) return listOf(main)
        val early = main.copy(
            id = "${main.id}#forvarsel",
            fireAtMillis = earlyAt,
            body = "Om ${formatMinutes(alarm.earlyWarningMinutes)}, kl. ${formatTime(alarm.time)}",
        )
        return listOf(early, main)
    }
}
