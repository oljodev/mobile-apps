package no.jodal.salat.apps.ekko

import kotlin.math.roundToInt

/** «0:07» eller «1:23». */
fun formatClock(seconds: Double): String {
    val total = seconds.coerceAtLeast(0.0).toInt()
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}

/** «0:07.4», med tideler, for klipping. */
fun formatPrecise(seconds: Double): String {
    val tenths = (seconds.coerceAtLeast(0.0) * 10).roundToInt()
    return "${formatClock(tenths / 10.0)}.${tenths % 10}"
}

/** «7,4 s» for korte lyder, ellers «1:23». */
fun formatLength(seconds: Double): String {
    if (seconds >= 60) return formatClock(seconds)
    val tenths = (seconds.coerceAtLeast(0.0) * 10).roundToInt()
    return "${tenths / 10},${tenths % 10} s"
}

/** «12 lyder · 3 min». */
fun formatCount(count: Int, totalSeconds: Double): String {
    val sounds = if (count == 1) "1 lyd" else "$count lyder"
    if (count == 0) return sounds
    val minutes = (totalSeconds / 60).roundToInt()
    return if (minutes < 1) "$sounds · ${totalSeconds.roundToInt()} s" else "$sounds · $minutes min"
}
