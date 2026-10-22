package no.jodal.salat.apps.brems

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.toLocalDateTime

/** Bruken én dag: hvor lenge, hvor ofte, og hvor mange ganger man lot være. */
data class DayUsage(
    val date: LocalDate,
    val millis: Long,
    val opens: Int,
    val pauses: Int,
    val stopped: Int,
    /** Tiden i hver app, mest brukt først. */
    val apps: List<Pair<String, Long>>,
)

/** Bruken på [date], med økta som pågår fram til [now]. */
fun BremsData.usageOn(date: LocalDate, now: Long, zone: TimeZone): DayUsage {
    val from = date.atStartOfDayIn(zone).toEpochMilliseconds()
    val to = date.plus(DatePeriod(days = 1)).atStartOfDayIn(zone).toEpochMilliseconds()
    val all = sessions + listOfNotNull(active?.let { Session(it.app, it.startMillis, minOf(now, it.startMillis + BremsRules.MaxSessionMillis)) })
    val perApp = all
        .map { it.app to (minOf(it.endMillis, to) - maxOf(it.startMillis, from)).coerceAtLeast(0) }
        .filter { it.second > 0 }
        .groupBy({ it.first }, { it.second })
        .map { (app, parts) -> app to parts.sum() }
        .sortedByDescending { it.second }
    val dayPauses = pauses.filter { it.startMillis in from until to }
    return DayUsage(
        date = date,
        millis = perApp.sumOf { it.second },
        opens = opens.count { it.atMillis in from until to },
        pauses = dayPauses.size,
        stopped = dayPauses.count { it.outcome == PauseOutcome.Stopped },
        apps = perApp,
    )
}

/** Når på [date] appene var åpne, som deler av døgnet fra 0 til 1. */
fun BremsData.segmentsOn(date: LocalDate, now: Long, zone: TimeZone): List<ClosedFloatingPointRange<Float>> {
    val from = date.atStartOfDayIn(zone).toEpochMilliseconds()
    val to = date.plus(DatePeriod(days = 1)).atStartOfDayIn(zone).toEpochMilliseconds()
    val all = sessions + listOfNotNull(active?.let { Session(it.app, it.startMillis, minOf(now, it.startMillis + BremsRules.MaxSessionMillis)) })
    val length = (to - from).toFloat()
    return all
        .filter { it.endMillis > from && it.startMillis < to }
        .map { ((maxOf(it.startMillis, from) - from) / length)..((minOf(it.endMillis, to) - from) / length) }
}

/** Hvor langt dagen har kommet, fra 0 ved midnatt til 1. */
fun dayFraction(now: Long, zone: TimeZone): Float {
    val date = today(now, zone)
    val from = date.atStartOfDayIn(zone).toEpochMilliseconds()
    val to = date.plus(DatePeriod(days = 1)).atStartOfDayIn(zone).toEpochMilliseconds()
    return (now - from).toFloat() / (to - from)
}

fun BremsData.usageToday(now: Long, zone: TimeZone): DayUsage = usageOn(today(now, zone), now, zone)

fun today(now: Long, zone: TimeZone): LocalDate = Instant.fromEpochMilliseconds(now).toLocalDateTime(zone).date

/** «47 min», «1 t 5 min», «0 min». */
fun formatDuration(millis: Long): String {
    val minutes = millis / 60_000
    return if (minutes < 60) "$minutes min" else "${minutes / 60} t ${minutes % 60} min"
}
