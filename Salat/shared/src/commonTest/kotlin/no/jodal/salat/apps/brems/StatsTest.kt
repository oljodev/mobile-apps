package no.jodal.salat.apps.brems

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class StatsTest {
    private val zone = TimeZone.UTC
    private val min = 60_000L

    /** Torsdag 22. oktober 2026 og dagene før. */
    private fun at(day: Int, hour: Int, minute: Int = 0) = LocalDateTime(2026, 10, day, hour, minute).toInstant(zone).toEpochMilliseconds()

    private fun session(app: String, day: Int, hour: Int, minutes: Int) = Session(app, at(day, hour), at(day, hour) + minutes * min)

    private fun pause(day: Int, kind: PauseKind, outcome: PauseOutcome, why: String? = null) =
        Pause("p$day$kind$outcome$why", "TikTok", kind, at(day, 20), at(day, 20) + 10_000, why = why, outcome = outcome)

    private val data = BremsData(
        opens = listOf(OpenEvent("TikTok", at(22, 21)), OpenEvent("TikTok", at(20, 21)), OpenEvent("YouTube", at(21, 8))),
        sessions = listOf(
            session("TikTok", 22, 21, 30),
            session("TikTok", 20, 21, 12),
            session("YouTube", 21, 8, 6),
            session("TikTok", 10, 21, 50),
        ),
        pauses = listOf(
            pause(22, PauseKind.Open, PauseOutcome.Stopped, "Vane"),
            pause(21, PauseKind.Open, PauseOutcome.Continued, "Vane"),
            pause(20, PauseKind.Limit, PauseOutcome.Stopped, "Kjeder meg"),
        ),
    )

    private val week = data.stats(StatsRange.Week, at(22, 23), zone)

    @Test
    fun sumsTheWeek() {
        assertEquals(48 * min, week.totalMillis)
        assertEquals(7, week.days.size)
        assertEquals(3, week.opens)
        assertEquals(listOf("TikTok", "YouTube"), week.apps.map { it.app })
        assertEquals(2, week.apps.first().opens)
    }

    @Test
    fun comparesWithTheWeekBefore() {
        assertEquals(50 * min, week.previousTotalMillis)
        assertEquals(-0.04f, week.trend!!, 0.001f)
    }

    @Test
    fun noTrendWithoutOlderData() {
        val fresh = BremsData(sessions = listOf(session("TikTok", 22, 21, 30)), opens = listOf(OpenEvent("TikTok", at(22, 21))))
        assertNull(fresh.stats(StatsRange.Week, at(22, 23), zone).trend)
    }

    @Test
    fun countsReasonsAndPauses() {
        assertEquals(listOf("Vane" to 2, "Kjeder meg" to 1), week.reasons)
        assertEquals(PauseStats(2, 1), week.openPauses)
        assertEquals(PauseStats(1, 1), week.longPauses)
    }

    @Test
    fun findsThePeakHourAndSessions() {
        assertEquals(21, week.peakHour())
        assertEquals(3, week.sessions)
        assertEquals(30 * min, week.longestSession!!.millis)
        assertEquals(2, week.overLimit)
    }
}
