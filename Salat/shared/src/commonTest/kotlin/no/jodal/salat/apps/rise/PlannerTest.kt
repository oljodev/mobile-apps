package no.jodal.salat.apps.rise

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone

class PlannerTest {
    private val monday = LocalDate(2026, 10, 19)
    private val now = LocalDateTime(monday, LocalTime(12, 0))
    private val oslo = TimeZone.of("Europe/Oslo")

    private fun alarm(id: String, kind: AlarmKind, repeat: Repeat = Repeat.Weekly((1..7).toSet())) = RiseAlarm(
        id = id,
        title = "",
        groupId = "annet",
        kind = kind,
        hour = 18,
        minute = 0,
        repeat = repeat,
        startDate = monday,
    )

    @Test
    fun plansTheWholeWindow() {
        val plan = RisePlanner.plan(RiseData(alarms = listOf(alarm("a", AlarmKind.Alarm))), now, oslo)
        assertEquals(RisePlanner.WindowDays + 1, plan.size)
        assertEquals("a@2026-10-19T18:00", plan.first().id)
        assertEquals("Alarm", plan.first().title)
    }

    @Test
    fun disabledAlarmsAreNotPlanned() {
        val a = alarm("a", AlarmKind.Alarm).copy(enabled = false)
        assertEquals(emptyList(), RisePlanner.plan(RiseData(alarms = listOf(a)), now, oslo))
    }

    @Test
    fun respectsTheLimits() {
        val alarms = (1..5).map { alarm("a$it", AlarmKind.Alarm) } + (1..5).map { alarm("n$it", AlarmKind.Notification) }
        val plan = RisePlanner.plan(RiseData(alarms = alarms), now, oslo)
        assertEquals(RisePlanner.MaxAlarms, plan.count { it.kind == AlarmKind.Alarm })
        assertEquals(RisePlanner.MaxNotifications, plan.count { it.kind == AlarmKind.Notification })
    }

    @Test
    fun earlyWarningComesBeforeTheNotification() {
        val n = alarm("n", AlarmKind.Notification, Repeat.Once(monday)).copy(earlyWarningMinutes = 60)
        val plan = RisePlanner.plan(RiseData(alarms = listOf(n)), now, oslo)
        assertEquals(listOf("n@2026-10-19T18:00#forvarsel", "n@2026-10-19T18:00"), plan.map { it.id })
        assertEquals("Om 1 time, kl. 18:00", plan.first().body)
        assertEquals(60 * 60_000L, plan[1].fireAtMillis - plan[0].fireAtMillis)
    }

    @Test
    fun earlyWarningInThePastIsLeftOut() {
        val n = alarm("n", AlarmKind.Notification, Repeat.Once(monday)).copy(earlyWarningMinutes = 1440)
        val plan = RisePlanner.plan(RiseData(alarms = listOf(n)), now, oslo)
        assertEquals(listOf("n@2026-10-19T18:00"), plan.map { it.id })
    }

    @Test
    fun snoozeOnlyAppliesToAlarms() {
        val n = alarm("n", AlarmKind.Notification, Repeat.Once(monday)).copy(snoozeMinutes = 10)
        val item = RisePlanner.plan(RiseData(alarms = listOf(n)), now, oslo).single()
        assertEquals(0, item.snoozeMinutes)
    }

    @Test
    fun summaries() {
        assertEquals("Hverdager", Repeat.Weekly(setOf(5, 1, 2, 3, 4)).summary(monday))
        assertEquals("Annenhver uke: tir, tor", Repeat.Weekly(setOf(2, 4), everyOtherWeek = true).summary(monday))
        assertEquals("I morgen", Repeat.Once(LocalDate(2026, 10, 20)).summary(monday))
        assertEquals("Ons 21. okt", Repeat.Once(LocalDate(2026, 10, 21)).summary(monday))
        assertEquals("Siste fredag hver måned", Repeat.MonthlyOnWeekday(-1, 5).summary(monday))
        assertTrue(Repeat.MonthlyOnDay(15).summary(monday).startsWith("Den 15."))
    }
}
