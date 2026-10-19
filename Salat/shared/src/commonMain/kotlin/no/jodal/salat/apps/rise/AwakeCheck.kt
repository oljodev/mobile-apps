package no.jodal.salat.apps.rise

import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * «Sjekk at jeg er oppe»: Etter en vekkealarm ringer Rise noen ganger til, med noen minutters mellomrom,
 * helt til man trykker «Jeg er oppe». Oppfølgingsalarmene planlegges sammen med selve alarmen, så det
 * virker selv om appen ikke kjører.
 */
object AwakeCheck {
    const val FollowUps = 3
    const val IntervalMinutes = 5

    /** Minutter etter selve alarmen at oppfølging nummer [index] (0, 1, 2, …) ringer. Venter til slumringen er over. */
    fun followUpDelayMinutes(alarm: RiseAlarm, index: Int): Int = alarm.snoozeMinutes + IntervalMinutes * (index + 1)

    /** Hvor lenge etter alarmen man blir spurt om man er oppe. Litt lenger enn siste oppfølging. */
    fun windowMinutes(alarm: RiseAlarm): Int = followUpDelayMinutes(alarm, FollowUps - 1) + IntervalMinutes
}

/** En nøkkel for én bestemt gang en alarm ringer, f.eks. `abc123@2026-10-20T06:30`. */
fun occurrenceKey(alarm: RiseAlarm, at: LocalDateTime): String = "${alarm.id}@$at"

/** Vekkealarmen som venter på «Jeg er oppe» akkurat nå, og når den ringte. */
fun RiseData.pendingAwakeCheck(now: LocalDateTime, timeZone: TimeZone): Pair<RiseAlarm, LocalDateTime>? {
    val nowInstant = now.toInstant(timeZone)
    val since = (nowInstant - 2.hours).toLocalDateTime(timeZone)
    return alarms
        .filter { it.enabled && it.kind == AlarmKind.Alarm && it.awakeCheck }
        .flatMap { alarm -> alarm.occurrences(since, now.date).map { alarm to it } }
        .filter { (alarm, at) ->
            val rang = at.toInstant(timeZone)
            rang <= nowInstant &&
                nowInstant < rang + AwakeCheck.windowMinutes(alarm).minutes &&
                occurrenceKey(alarm, at) !in confirmedAwake
        }
        .maxByOrNull { it.second }
}

/** Fjerner bekreftelser som er over et døgn gamle. */
fun RiseData.withoutOldConfirmations(now: LocalDateTime): RiseData {
    val cutoff = now.date.toString()
    return copy(confirmedAwake = confirmedAwake.filter { it.substringAfter('@') >= cutoff }.toSet())
}
