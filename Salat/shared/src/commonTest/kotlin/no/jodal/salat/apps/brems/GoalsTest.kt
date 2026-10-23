package no.jodal.salat.apps.brems

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class GoalsTest {
    private val zone = TimeZone.UTC
    private val min = 60_000L

    private fun at(day: Int, hour: Int) = LocalDateTime(2026, 10, day, hour, 0).toInstant(zone).toEpochMilliseconds()

    private fun session(app: String, day: Int, minutes: Int) = Session(app, at(day, 12), at(day, 12) + minutes * min)

    private val goal = Goal("g", "Snapchat", 15, LocalDate(2026, 10, 18))

    @Test
    fun countsDaysInARowUnderTheGoal() {
        val data = BremsData(
            goals = listOf(goal),
            sessions = listOf(session("Snapchat", 18, 30), session("Snapchat", 19, 10), session("Snapchat", 20, 14), session("TikTok", 21, 90)),
        )
        val status = data.goalStatus(goal, at(22, 9), zone)
        // 19., 20., 21. (TikTok teller ikke) og i dag.
        assertEquals(4, status.streak)
        assertEquals(DayResult.Missed, status.week.first { it.first == LocalDate(2026, 10, 18) }.second)
        assertEquals(DayResult.OnTrack, status.week.last().second)
    }

    @Test
    fun goingOverTodayBreaksTheStreak() {
        val data = BremsData(goals = listOf(goal), sessions = listOf(session("Snapchat", 21, 5), session("Snapchat", 22, 20)))
        val status = data.goalStatus(goal, at(22, 18), zone)
        assertEquals(0, status.streak)
        assertEquals(true, status.reached)
    }

    @Test
    fun daysBeforeTheGoalDoNotCount() {
        val data = BremsData(goals = listOf(goal))
        val status = data.goalStatus(goal, at(19, 9), zone)
        assertEquals(2, status.streak)
        assertEquals(DayResult.Before, status.week.first().second)
    }

    @Test
    fun aGoalForAllAppsCountsEveryApp() {
        val all = Goal("a", "", 30, LocalDate(2026, 10, 18))
        val data = BremsData(goals = listOf(all), sessions = listOf(session("Snapchat", 22, 20), session("TikTok", 22, 15)))
        assertEquals(35 * min, data.goalStatus(all, at(22, 18), zone).usedTodayMillis)
    }

    @Test
    fun aReachedGoalGivesTheLongPause() {
        val data = BremsData(goals = listOf(goal), sessions = listOf(session("Snapchat", 22, 16)))
        val (_, decision) = BremsRules.opened(data, "Snapchat", at(22, 18), zone)
        assertEquals(PauseKind.Goal, assertIs<Decision.Wait>(decision).pause.kind)
        assertNull(data.reachedGoal("TikTok", at(22, 18), zone))
    }
}
