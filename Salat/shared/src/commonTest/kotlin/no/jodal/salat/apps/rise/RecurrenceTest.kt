package no.jodal.salat.apps.rise

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

class RecurrenceTest {
    // 2026-10-19 er en mandag.
    private val monday = LocalDate(2026, 10, 19)

    private fun alarm(repeat: Repeat, start: LocalDate = monday, end: LocalDate? = null, skipped: Set<LocalDate> = emptySet()) =
        RiseAlarm(
            id = "a",
            title = "Test",
            groupId = WakeUpGroupId,
            kind = AlarmKind.Alarm,
            hour = 6,
            minute = 30,
            repeat = repeat,
            startDate = start,
            endDate = end,
            skippedDates = skipped,
        )

    private fun dates(alarm: RiseAlarm, after: LocalDateTime, until: LocalDate) =
        alarm.occurrences(after, until).map { it.date }

    private fun at(date: LocalDate, hour: Int = 0, minute: Int = 0) = LocalDateTime(date, LocalTime(hour, minute))

    @Test
    fun onceRingsOnlyOnItsDate() {
        val a = alarm(Repeat.Once(LocalDate(2026, 10, 21)))
        assertEquals(listOf(LocalDate(2026, 10, 21)), dates(a, at(monday), LocalDate(2026, 12, 31)))
    }

    @Test
    fun onceInThePastDoesNotRing() {
        val a = alarm(Repeat.Once(monday))
        assertEquals(emptyList(), dates(a, at(monday, 7), LocalDate(2026, 12, 31)))
    }

    @Test
    fun laterTodayCountsButEarlierTodayDoesNot() {
        val a = alarm(Repeat.Weekly(setOf(1)))
        assertEquals(monday, a.nextOccurrence(at(monday, 6, 0))?.date)
        assertEquals(LocalDate(2026, 10, 26), a.nextOccurrence(at(monday, 6, 30))?.date)
    }

    @Test
    fun weekdays() {
        val a = alarm(Repeat.Weekly(setOf(1, 2, 3, 4, 5)))
        val result = dates(a, at(LocalDate(2026, 10, 23), 7), LocalDate(2026, 10, 27))
        assertEquals(listOf(LocalDate(2026, 10, 26), LocalDate(2026, 10, 27)), result)
    }

    @Test
    fun everyOtherWeekCountsFromTheStartWeek() {
        // Start på en onsdag. Tirsdagen samme uke er før start, så første gang er tirsdag to uker senere.
        val a = alarm(Repeat.Weekly(setOf(2), everyOtherWeek = true), start = LocalDate(2026, 10, 21))
        val result = dates(a, at(monday), LocalDate(2026, 11, 30))
        assertEquals(
            listOf(LocalDate(2026, 11, 3), LocalDate(2026, 11, 17)),
            result,
        )
    }

    @Test
    fun monthlyOnDayUsesLastDayInShortMonths() {
        val a = alarm(Repeat.MonthlyOnDay(31))
        val result = dates(a, at(monday), LocalDate(2027, 2, 28))
        assertEquals(
            listOf(
                LocalDate(2026, 10, 31),
                LocalDate(2026, 11, 30),
                LocalDate(2026, 12, 31),
                LocalDate(2027, 1, 31),
                LocalDate(2027, 2, 28),
            ),
            result,
        )
    }

    @Test
    fun firstMondayOfTheMonth() {
        val a = alarm(Repeat.MonthlyOnWeekday(week = 1, dayOfWeek = 1))
        val result = dates(a, at(monday), LocalDate(2027, 1, 31))
        assertEquals(listOf(LocalDate(2026, 11, 2), LocalDate(2026, 12, 7), LocalDate(2027, 1, 4)), result)
    }

    @Test
    fun lastFridayOfTheMonth() {
        val a = alarm(Repeat.MonthlyOnWeekday(week = Repeat.MonthlyOnWeekday.LastWeek, dayOfWeek = 5))
        val result = dates(a, at(monday), LocalDate(2026, 12, 31))
        assertEquals(listOf(LocalDate(2026, 10, 30), LocalDate(2026, 11, 27), LocalDate(2026, 12, 25)), result)
    }

    @Test
    fun startAndEndDate() {
        val a = alarm(
            Repeat.Weekly(setOf(1)),
            start = LocalDate(2026, 11, 1),
            end = LocalDate(2026, 11, 16),
        )
        val result = dates(a, at(monday), LocalDate(2026, 12, 31))
        assertEquals(listOf(LocalDate(2026, 11, 2), LocalDate(2026, 11, 9), LocalDate(2026, 11, 16)), result)
    }

    @Test
    fun skippedDatesAreLeftOut() {
        val a = alarm(Repeat.Weekly(setOf(1)), skipped = setOf(LocalDate(2026, 10, 26)))
        val result = dates(a, at(monday, 7), LocalDate(2026, 11, 9))
        assertEquals(listOf(LocalDate(2026, 11, 2), LocalDate(2026, 11, 9)), result)
    }

    @Test
    fun disabledAlarmHasNoNextOccurrence() {
        val a = alarm(Repeat.Weekly(setOf(1))).copy(enabled = false)
        assertEquals(null, a.nextOccurrence(at(monday)))
    }
}
