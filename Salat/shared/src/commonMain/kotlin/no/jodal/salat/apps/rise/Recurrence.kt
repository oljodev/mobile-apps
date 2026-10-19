package no.jodal.salat.apps.rise

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

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
