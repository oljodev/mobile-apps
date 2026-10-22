package no.jodal.salat.apps.rise

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant

/** Om alarmen skal ringe på [date], før man tar hensyn til start, slutt og hoppede dager. */
fun Repeat.matches(date: LocalDate, startDate: LocalDate): Boolean = when (this) {
    is Repeat.Once -> date == this.date

    is Repeat.Weekly -> date.dayOfWeek.isoDayNumber in days &&
        (!everyOtherWeek || weeksBetween(startDate, date) % 2 == 0)

    is Repeat.MonthlyOnDay -> date.day == minOf(day, date.lengthOfMonth())

    is Repeat.MonthlyOnWeekday -> date.dayOfWeek.isoDayNumber == dayOfWeek &&
        if (week == Repeat.MonthlyOnWeekday.LastWeek) {
            date.plus(DatePeriod(days = 7)).month != date.month
        } else {
            (date.day - 1) / 7 + 1 == week
        }
}

/** Om en alarm som bare går én gang, har ringt. Den vises da ikke lenger i Rise. */
fun RiseAlarm.hasRung(now: LocalDateTime): Boolean = repeat is Repeat.Once && LocalDateTime(repeat.date, time) <= now

/**
 * Hvor lenge en engangsalarm blir liggende etter at den har ringt, før den slettes. Slettes den med
 * en gang, stopper iOS slumringen, siden alarmen ikke finnes lenger.
 */
val KeepAfterRinging: Duration = 2.hours

/** Om en engangsalarm har ringt for så lenge siden at den kan slettes. */
fun RiseAlarm.isDone(now: LocalDateTime, timeZone: TimeZone): Boolean =
    repeat is Repeat.Once && LocalDateTime(repeat.date, time).toInstant(timeZone) + KeepAfterRinging <= now.toInstant(timeZone)

/** Alle gangene alarmen ringer etter [after], til og med [until]. */
fun RiseAlarm.occurrences(after: LocalDateTime, until: LocalDate): List<LocalDateTime> {
    val first = if (repeat is Repeat.Once) after.date else maxOf(after.date, startDate)
    val last = if (repeat is Repeat.Once) until else endDate?.let { minOf(it, until) } ?: until
    return generateSequence(first) { it.plus(DatePeriod(days = 1)) }
        .takeWhile { it <= last }
        .filter { it !in skippedDates && repeat.matches(it, startDate) }
        .map { LocalDateTime(it, time) }
        .filter { it > after }
        .toList()
}

/** Neste gang alarmen ringer, eller `null` hvis den ikke ringer igjen innen ett år. */
fun RiseAlarm.nextOccurrence(after: LocalDateTime): LocalDateTime? {
    if (!enabled) return null
    val until = after.date.plus(DatePeriod(years = 1))
    return occurrences(after, until).firstOrNull()
}

/** Antall hele uker mellom mandagen i uken til [from] og mandagen i uken til [to]. */
private fun weeksBetween(from: LocalDate, to: LocalDate): Int =
    from.mondayOfWeek().daysUntil(to.mondayOfWeek()) / 7

private fun LocalDate.mondayOfWeek(): LocalDate = minus(DatePeriod(days = dayOfWeek.isoDayNumber - 1))

private fun LocalDate.lengthOfMonth(): Int {
    val firstOfNext = LocalDate(year, month, 1).plus(DatePeriod(months = 1))
    return firstOfNext.minus(DatePeriod(days = 1)).day
}

/** ISO-nummeret til en ukedag, som en [DayOfWeek]. */
fun isoDay(number: Int): DayOfWeek = DayOfWeek(number)
