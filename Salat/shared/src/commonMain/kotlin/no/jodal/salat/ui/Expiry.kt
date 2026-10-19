package no.jodal.salat.ui

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import no.jodal.salat.NativeBridge

/** Hvor lenge det er til SideStore-signeringen går ut, eller `null` hvis ukjent. */
fun NativeBridge.signingTimeLeft(): Duration? =
    signingExpiresAtMillis()?.let { (it - Clock.System.now().toEpochMilliseconds()).milliseconds }

/** F.eks. «5 dager og 3 timer». */
fun formatTimeLeft(timeLeft: Duration): String {
    val days = timeLeft.inWholeDays
    val hours = timeLeft.inWholeHours % 24
    val dayText = if (days == 1L) "1 dag" else "$days dager"
    val hourText = if (hours == 1L) "1 time" else "$hours timer"
    return if (days > 0) "$dayText og $hourText" else hourText
}
