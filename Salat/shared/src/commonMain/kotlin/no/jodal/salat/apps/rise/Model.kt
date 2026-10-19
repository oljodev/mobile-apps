package no.jodal.salat.apps.rise

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Alt Rise lagrer. */
@Serializable
data class RiseData(
    val groups: List<RiseGroup> = DefaultGroups,
    val alarms: List<RiseAlarm> = emptyList(),
    /** Vekkealarmer man har trykket «Jeg er oppe» på, som nøkler fra [occurrenceKey]. */
    val confirmedAwake: Set<String> = emptySet(),
)

@Serializable
data class RiseGroup(
    val id: String,
    val name: String,
)

val DefaultGroups = listOf(
    RiseGroup(id = "vekking", name = "Vekking"),
    RiseGroup(id = "trening", name = "Trening"),
    RiseGroup(id = "annet", name = "Annet"),
)

/** Gruppen der «Sjekk at jeg er oppe» er på som standard. */
const val WakeUpGroupId = "vekking"

@Serializable
enum class AlarmKind {
    /** Ringer gjennom lydløs og Fokus. */
    @SerialName("alarm") Alarm,

    /** Et vanlig varsel med lyd. */
    @SerialName("notification") Notification,
}

@Serializable
data class RiseAlarm(
    val id: String,
    val title: String,
    val groupId: String,
    val kind: AlarmKind,
    val hour: Int,
    val minute: Int,
    val repeat: Repeat,
    val enabled: Boolean = true,
    val sound: String = RiseSound.Default.id,
    /** Minutter, eller 0 for ingen slumring. Gjelder bare alarmer. */
    val snoozeMinutes: Int = 10,
    /** Ring igjen noen ganger etter alarmen, til man trykker «Jeg er oppe». Gjelder bare alarmer. */
    val awakeCheck: Boolean = false,
    /** Minutter før tidspunktet et ekstra varsel skal komme, eller 0 for ingen. Gjelder bare varsler. */
    val earlyWarningMinutes: Int = 0,
    /** Første dag en gjentakelse gjelder. Brukes også til å telle «annenhver uke». */
    val startDate: LocalDate,
    /** Siste dag en gjentakelse gjelder, eller `null` for ingen slutt. */
    val endDate: LocalDate? = null,
    /** Enkeltdager som er hoppet over. */
    val skippedDates: Set<LocalDate> = emptySet(),
) {
    val time: LocalTime get() = LocalTime(hour, minute)
}

@Serializable
sealed interface Repeat {
    /** Én gang, på en bestemt dato. */
    @Serializable
    @SerialName("once")
    data class Once(val date: LocalDate) : Repeat

    /** Valgte ukedager, som ISO-numre (1 = mandag, 7 = søndag). */
    @Serializable
    @SerialName("weekly")
    data class Weekly(val days: Set<Int>, val everyOtherWeek: Boolean = false) : Repeat

    /** En bestemt dag i måneden. Finnes ikke dagen, brukes siste dag i måneden. */
    @Serializable
    @SerialName("monthly-day")
    data class MonthlyOnDay(val day: Int) : Repeat

    /** F.eks. første mandag i måneden. [week] er 1–4, eller [LastWeek] for siste. */
    @Serializable
    @SerialName("monthly-weekday")
    data class MonthlyOnWeekday(val week: Int, val dayOfWeek: Int) : Repeat {
        companion object {
            const val LastWeek = -1
        }
    }
}

/** De innebygde lydene. [id] er filnavnet uten filendelse i iOS-appen. */
enum class RiseSound(val id: String, val label: String, val description: String) {
    Default("standard", "Standard", "Systemets alarmlyd"),
    Gentle("rise-myk", "Myk morgen", "Rolig og stigende"),
    Chimes("rise-klokkespill", "Klokkespill", "Lyse klokker"),
    Beep("rise-pip", "Pip", "Klassisk pip-pip"),
    Loud("rise-kraftig", "Kraftig", "For dem som sover tungt"),
    ;

    companion object {
        fun byId(id: String): RiseSound = entries.firstOrNull { it.id == id } ?: Default
    }
}
