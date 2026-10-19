package no.jodal.salat.apps.rise

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone

class AwakeCheckTest {
    private val monday = LocalDate(2026, 10, 19)
    private val oslo = TimeZone.of("Europe/Oslo")

    private val wakeUp = RiseAlarm(
        id = "a",
        title = "Stå opp",
        groupId = WakeUpGroupId,
        kind = AlarmKind.Alarm,
        hour = 6,
        minute = 30,
        repeat = Repeat.Once(monday),
        snoozeMinutes = 10,
        awakeCheck = true,
        startDate = monday,
    )

    private fun at(hour: Int, minute: Int) = LocalDateTime(monday, LocalTime(hour, minute))

    private fun minutesAfterAlarm(item: PlannedItem, data: RiseData) =
        (item.fireAtMillis - RisePlanner.plan(data, at(6, 0), oslo).first().fireAtMillis) / 60_000

    @Test
    fun followUpsWaitForSnoozeThenComeEveryFiveMinutes() {
        val data = RiseData(alarms = listOf(wakeUp))
        val plan = RisePlanner.plan(data, at(6, 0), oslo)
        assertEquals(listOf("a@2026-10-19T06:30", "a@2026-10-19T06:30#oppe1", "a@2026-10-19T06:30#oppe2", "a@2026-10-19T06:30#oppe3"), plan.map { it.id })
        assertEquals(listOf(0L, 15L, 20L, 25L), plan.map { minutesAfterAlarm(it, data) })
        assertEquals("Er du oppe?", plan[1].title)
        assertEquals(0, plan[1].snoozeMinutes)
    }

    @Test
    fun followUpsStillComeAfterTheAlarmHasRung() {
        val plan = RisePlanner.plan(RiseData(alarms = listOf(wakeUp)), at(6, 47), oslo)
        assertEquals(listOf("a@2026-10-19T06:30#oppe2", "a@2026-10-19T06:30#oppe3"), plan.map { it.id })
    }

    @Test
    fun confirmingRemovesTheFollowUps() {
        val data = RiseData(alarms = listOf(wakeUp), confirmedAwake = setOf("a@2026-10-19T06:30"))
        assertEquals(emptyList(), RisePlanner.plan(data, at(6, 40), oslo))
    }

    @Test
    fun noFollowUpsWithoutTheAwakeCheck() {
        val data = RiseData(alarms = listOf(wakeUp.copy(awakeCheck = false)))
        assertEquals(listOf("a@2026-10-19T06:30"), RisePlanner.plan(data, at(6, 0), oslo).map { it.id })
    }

    @Test
    fun pendingFromTheAlarmUntilAfterTheLastFollowUp() {
        val data = RiseData(alarms = listOf(wakeUp))
        assertNull(data.pendingAwakeCheck(at(6, 29), oslo))
        assertEquals(at(6, 30), data.pendingAwakeCheck(at(6, 30), oslo)?.second)
        assertEquals(at(6, 30), data.pendingAwakeCheck(at(6, 59), oslo)?.second)
        assertNull(data.pendingAwakeCheck(at(7, 0), oslo))
    }

    @Test
    fun notPendingOnceConfirmed() {
        val data = RiseData(alarms = listOf(wakeUp), confirmedAwake = setOf("a@2026-10-19T06:30"))
        assertNull(data.pendingAwakeCheck(at(6, 35), oslo))
    }

    @Test
    fun oldConfirmationsAreRemoved() {
        val data = RiseData(confirmedAwake = setOf("a@2026-10-17T06:30", "a@2026-10-19T06:30"))
        assertEquals(setOf("a@2026-10-19T06:30"), data.withoutOldConfirmations(at(12, 0)).confirmedAwake)
    }
}
