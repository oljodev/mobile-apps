package no.jodal.salat.apps.rise

import kotlin.time.Duration
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus

private val ShortDays = listOf("man", "tir", "ons", "tor", "fre", "lør", "søn")
private val LongDays = listOf("mandag", "tirsdag", "onsdag", "torsdag", "fredag", "lørdag", "søndag")
private val ShortMonths = listOf("jan", "feb", "mar", "apr", "mai", "jun", "jul", "aug", "sep", "okt", "nov", "des")
private val WeekNames = listOf("Første", "Andre", "Tredje", "Fjerde")

fun formatTime(time: LocalTime): String =
    "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"

fun shortDayName(isoDay: Int): String = ShortDays[isoDay - 1]

fun longDayName(isoDay: Int): String = LongDays[isoDay - 1]

/** F.eks. «tir 21. okt», eller «i dag» og «i morgen». */
fun formatDate(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "i dag"
    today.plus(DatePeriod(days = 1)) -> "i morgen"
    else -> "${shortDayName(date.dayOfWeek.isoDayNumber)} ${date.day}. ${ShortMonths[date.month.ordinal]}" +
        if (date.year != today.year) " ${date.year}" else ""
}

fun formatMinutes(minutes: Int): String = when {
    minutes % 1440 == 0 -> if (minutes == 1440) "1 døgn" else "${minutes / 1440} døgn"
    minutes % 60 == 0 -> if (minutes == 60) "1 time" else "${minutes / 60} timer"
    else -> "$minutes min"
}

/** F.eks. «om 8 t 12 min». */
fun formatCountdown(duration: Duration): String {
    val totalMinutes = duration.inWholeMinutes + 1
    val days = totalMinutes / 1440
    val hours = totalMinutes % 1440 / 60
    val minutes = totalMinutes % 60
    return when {
        days > 0 -> "om ${days} d ${hours} t"
        hours > 0 -> "om ${hours} t ${minutes} min"
        else -> "om ${minutes} min"
    }
}

fun Repeat.summary(today: LocalDate): String = when (this) {
    is Repeat.Once -> formatDate(date, today).replaceFirstChar { it.uppercase() }
    is Repeat.Weekly -> {
        val sorted = days.sorted()
        val dayText = when (sorted) {
            listOf(1, 2, 3, 4, 5, 6, 7) -> "Hver dag"
            listOf(1, 2, 3, 4, 5) -> "Hverdager"
            listOf(6, 7) -> "Helg"
            else -> sorted.joinToString(", ") { shortDayName(it) }.replaceFirstChar { it.uppercase() }
        }
        if (everyOtherWeek) "Annenhver uke: ${dayText.replaceFirstChar { it.lowercase() }}" else dayText
    }
    is Repeat.MonthlyOnDay -> "Den $day. hver måned"
    is Repeat.MonthlyOnWeekday -> {
        val week = if (week == Repeat.MonthlyOnWeekday.LastWeek) "Siste" else WeekNames[week - 1]
        "$week ${longDayName(dayOfWeek)} hver måned"
    }
}
