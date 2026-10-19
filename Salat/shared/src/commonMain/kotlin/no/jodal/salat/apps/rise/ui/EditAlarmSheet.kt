package no.jodal.salat.apps.rise.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DatePicker
import androidx.compose.material3.Icon
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Instant
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import no.jodal.salat.NativeBridge
import no.jodal.salat.apps.rise.AlarmKind
import no.jodal.salat.apps.rise.Repeat
import no.jodal.salat.apps.rise.RiseAlarm
import no.jodal.salat.apps.rise.RiseSound
import no.jodal.salat.apps.rise.RiseStore
import no.jodal.salat.apps.rise.WakeUpGroupId
import no.jodal.salat.apps.rise.formatDate
import no.jodal.salat.apps.rise.formatMinutes
import no.jodal.salat.apps.rise.longDayName
import no.jodal.salat.apps.rise.nextOccurrence
import no.jodal.salat.apps.rise.shortDayName

private val SnoozeOptions = listOf(0, 5, 10, 15)
private val EarlyWarningOptions = listOf(0, 15, 30, 60, 1440)
private val WeekOptions = listOf(1, 2, 3, 4, Repeat.MonthlyOnWeekday.LastWeek)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlarmSheet(initial: RiseAlarm, isNew: Boolean, store: RiseStore, bridge: NativeBridge, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun close(save: () -> Unit) {
        bridge.stopSoundPreview()
        save()
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    DisposableEffect(Unit) { onDispose { bridge.stopSoundPreview() } }

    ModalBottomSheet(
        onDismissRequest = { close {} },
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        EditAlarmContent(initial, isNew, store, bridge, onClose = ::close)
    }
}

/** Innholdet i [EditAlarmSheet]. [onClose] kalles med det som skal skje før arket lukkes. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditAlarmContent(initial: RiseAlarm, isNew: Boolean, store: RiseStore, bridge: NativeBridge, onClose: (save: () -> Unit) -> Unit) {
    var draft by remember { mutableStateOf(initial) }
    val now = store.now()
    val inThePast = draft.repeat is Repeat.Once && LocalDateTime((draft.repeat as Repeat.Once).date, draft.time) <= now
    val noDays = (draft.repeat as? Repeat.Weekly)?.days?.isEmpty() == true

    fun close(save: () -> Unit = {}) = onClose(save)

    Column {
        SheetHeader(
            title = if (isNew) "Ny alarm" else "Rediger",
            saveEnabled = !inThePast && !noDays,
            onCancel = { close() },
            onSave = {
                close {
                    store.update { data ->
                        val others = data.alarms.filter { it.id != draft.id }
                        data.copy(alarms = others + draft.copy(enabled = true))
                    }
                }
            },
        )
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            TimeWheel(draft.hour, draft.minute) { hour, minute -> draft = draft.copy(hour = hour, minute = minute) }

            SettingsSection(null) {
                NameField(draft.title, placeholder = if (draft.kind == AlarmKind.Alarm) "Navn, f.eks. Stå opp" else "Navn, f.eks. Tannlegen") {
                    draft = draft.copy(title = it)
                }
            }

            SettingsSection("Type") {
                SegmentedControl(
                    options = listOf("Alarm", "Varsel"),
                    selected = draft.kind.ordinal,
                    onSelect = { draft = draft.copy(kind = AlarmKind.entries[it]) },
                )
                Text(
                    if (draft.kind == AlarmKind.Alarm) {
                        "Ringer gjennom lydløs og Fokus, til du stopper den."
                    } else {
                        "Et vanlig varsel med lyd. Følger lydløs-bryteren."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SettingsSection("Gruppe") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (group in store.data.groups) {
                        ChipButton(group.name, selected = draft.groupId == group.id) {
                            draft = draft.copy(
                                groupId = group.id,
                                awakeCheck = if (isNew) group.id == WakeUpGroupId else draft.awakeCheck,
                            )
                        }
                    }
                }
            }

            RepeatSection(draft, now) { draft = it }
            if (inThePast) Warning("Tidspunktet har allerede vært. Velg en annen dato eller et annet klokkeslett.")
            if (noDays) Warning("Velg minst én dag.")

            if (!isNew && draft.repeat !is Repeat.Once) {
                SkipNextSection(draft, now) { draft = it }
            }

            SoundSection(draft.sound, onSelect = {
                draft = draft.copy(sound = it.id)
                bridge.previewSound(it.id)
            })

            if (draft.kind == AlarmKind.Alarm) {
                SettingsSection("Slumring") {
                    SegmentedControl(
                        options = SnoozeOptions.map { if (it == 0) "Av" else "$it min" },
                        selected = SnoozeOptions.indexOf(draft.snoozeMinutes).coerceAtLeast(0),
                        onSelect = { draft = draft.copy(snoozeMinutes = SnoozeOptions[it]) },
                    )
                    SectionDivider()
                    SwitchRow(
                        title = "Sjekk at jeg er oppe",
                        description = "Ringer igjen 5 minutter etter at du har stoppet alarmen, helt til du trykker «Jeg er oppe» i Rise.",
                        checked = draft.awakeCheck,
                        onCheckedChange = { draft = draft.copy(awakeCheck = it) },
                    )
                }
            } else {
                SettingsSection("Forvarsel") {
                    SegmentedControl(
                        options = EarlyWarningOptions.map { if (it == 0) "Av" else formatMinutes(it).replace(" time", " t").replace(" døgn", " d") },
                        selected = EarlyWarningOptions.indexOf(draft.earlyWarningMinutes).coerceAtLeast(0),
                        onSelect = { draft = draft.copy(earlyWarningMinutes = EarlyWarningOptions[it]) },
                    )
                    Text(
                        "Et ekstra varsel før selve tidspunktet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (!isNew) {
                TextButton(
                    onClick = { close { store.update { d -> d.copy(alarms = d.alarms.filter { it.id != draft.id }) } } },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Slett alarm", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
                }
            }
            Box(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SheetHeader(title: String, saveEnabled: Boolean, onCancel: () -> Unit, onSave: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        TextButton(onClick = onCancel) { Text("Avbryt", style = MaterialTheme.typography.bodyLarge) }
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        TextButton(onClick = onSave, enabled = saveEnabled) {
            Text("Lagre", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TimeWheel(hour: Int, minute: Int, onChange: (Int, Int) -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
        // Markeringen bak det valgte klokkeslettet.
        Box(
            Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            WheelPicker(
                count = 24,
                selected = hour,
                onSelected = { onChange(it, minute) },
                label = { it.toString().padStart(2, '0') },
                modifier = Modifier.width(90.dp),
            )
            Text(":", fontSize = 34.sp, fontWeight = FontWeight.Medium)
            WheelPicker(
                count = 60,
                selected = minute,
                onSelected = { onChange(hour, it) },
                label = { it.toString().padStart(2, '0') },
                modifier = Modifier.width(90.dp),
            )
        }
    }
}

@Composable
private fun NameField(value: String, placeholder: String, onChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                inner()
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RepeatSection(draft: RiseAlarm, now: LocalDateTime, onChange: (RiseAlarm) -> Unit) {
    val repeat = draft.repeat
    val mode = when {
        repeat is Repeat.Once -> 0
        repeat is Repeat.Weekly && !repeat.everyOtherWeek -> 1
        else -> 2
    }
    val nextDate = if (draft.time > now.time) now.date else now.date.plus(DatePeriod(days = 1))

    SettingsSection("Gjentakelse") {
        SegmentedControl(
            options = listOf("Én gang", "Ukedager", "Avansert"),
            selected = mode,
            onSelect = { selected ->
                if (selected == mode) return@SegmentedControl
                val newRepeat = when (selected) {
                    0 -> Repeat.Once(nextDate)
                    1 -> Repeat.Weekly((repeat as? Repeat.Weekly)?.days ?: setOf(1, 2, 3, 4, 5))
                    else -> Repeat.Weekly((repeat as? Repeat.Weekly)?.days ?: setOf(now.date.dayOfWeek.isoDayNumber), everyOtherWeek = true)
                }
                onChange(draft.copy(repeat = newRepeat, startDate = now.date, endDate = null))
            },
        )

        when (mode) {
            0 -> OnceOptions(repeat as Repeat.Once, now.date) { onChange(draft.copy(repeat = it)) }
            1 -> {
                val days = (repeat as Repeat.Weekly).days
                DayPicker(days) { onChange(draft.copy(repeat = repeat.copy(days = it))) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipButton("Hverdager", days == setOf(1, 2, 3, 4, 5)) { onChange(draft.copy(repeat = repeat.copy(days = setOf(1, 2, 3, 4, 5)))) }
                    ChipButton("Helg", days == setOf(6, 7)) { onChange(draft.copy(repeat = repeat.copy(days = setOf(6, 7)))) }
                    ChipButton("Hver dag", days.size == 7) { onChange(draft.copy(repeat = repeat.copy(days = (1..7).toSet()))) }
                }
            }
            else -> AdvancedOptions(draft, now.date, onChange)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnceOptions(repeat: Repeat.Once, today: LocalDate, onChange: (Repeat.Once) -> Unit) {
    val tomorrow = today.plus(DatePeriod(days = 1))
    var picking by remember { mutableStateOf(false) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChipButton("I dag", repeat.date == today) { onChange(Repeat.Once(today)) }
        ChipButton("I morgen", repeat.date == tomorrow) { onChange(Repeat.Once(tomorrow)) }
        val other = repeat.date != today && repeat.date != tomorrow
        ChipButton(if (other) formatDate(repeat.date, today).replaceFirstChar { it.uppercase() } else "Velg dato …", other) { picking = true }
    }
    if (picking) {
        DatePickerPopup(repeat.date, onDismiss = { picking = false }) { onChange(Repeat.Once(it)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdvancedOptions(draft: RiseAlarm, today: LocalDate, onChange: (RiseAlarm) -> Unit) {
    val repeat = draft.repeat
    val isMonthly = repeat is Repeat.MonthlyOnDay || repeat is Repeat.MonthlyOnWeekday
    SegmentedControl(
        options = listOf("Annenhver uke", "Månedlig"),
        selected = if (isMonthly) 1 else 0,
        onSelect = {
            val newRepeat = if (it == 0) {
                Repeat.Weekly(setOf(draft.startDate.dayOfWeek.isoDayNumber), everyOtherWeek = true)
            } else {
                Repeat.MonthlyOnDay(draft.startDate.day)
            }
            if ((it == 1) != isMonthly) onChange(draft.copy(repeat = newRepeat))
        },
    )

    when (repeat) {
        is Repeat.Weekly -> {
            DayPicker(repeat.days) { onChange(draft.copy(repeat = repeat.copy(days = it))) }
            Hint("Annenhver uke, regnet fra uken alarmen starter.")
        }
        is Repeat.MonthlyOnDay, is Repeat.MonthlyOnWeekday -> {
            SegmentedControl(
                options = listOf("Dato", "Ukedag"),
                selected = if (repeat is Repeat.MonthlyOnDay) 0 else 1,
                onSelect = {
                    val newRepeat = if (it == 0) {
                        Repeat.MonthlyOnDay(draft.startDate.day)
                    } else {
                        Repeat.MonthlyOnWeekday(week = 1, dayOfWeek = draft.startDate.dayOfWeek.isoDayNumber)
                    }
                    onChange(draft.copy(repeat = newRepeat))
                },
            )
            if (repeat is Repeat.MonthlyOnDay) {
                Stepper(
                    text = "Den ${repeat.day}. hver måned",
                    onMinus = { if (repeat.day > 1) onChange(draft.copy(repeat = Repeat.MonthlyOnDay(repeat.day - 1))) },
                    onPlus = { if (repeat.day < 31) onChange(draft.copy(repeat = Repeat.MonthlyOnDay(repeat.day + 1))) },
                )
                if (repeat.day > 28) Hint("I måneder uten den ${repeat.day}. ringer den siste dag i måneden.")
            } else if (repeat is Repeat.MonthlyOnWeekday) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (week in WeekOptions) {
                        val label = if (week == Repeat.MonthlyOnWeekday.LastWeek) "Siste" else "$week."
                        ChipButton(label, repeat.week == week) { onChange(draft.copy(repeat = repeat.copy(week = week))) }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (day in 1..7) {
                        ChipButton(shortDayName(day).replaceFirstChar { it.uppercase() }, repeat.dayOfWeek == day) {
                            onChange(draft.copy(repeat = repeat.copy(dayOfWeek = day)))
                        }
                    }
                }
                val weekText = if (repeat.week == Repeat.MonthlyOnWeekday.LastWeek) "Siste" else "${repeat.week}."
                Hint("$weekText ${longDayName(repeat.dayOfWeek)} i hver måned.")
            }
        }
        is Repeat.Once -> Unit
    }

    SectionDivider()
    DateRow("Starter", draft.startDate, today) { onChange(draft.copy(startDate = it)) }
    SwitchRow(
        title = "Sluttdato",
        description = draft.endDate?.let { "Siste gang ${formatDate(it, today)}" } ?: "Fortsetter til du skrur den av",
        checked = draft.endDate != null,
        onCheckedChange = { on -> onChange(draft.copy(endDate = if (on) draft.startDate.plus(DatePeriod(months = 2)) else null)) },
    )
    draft.endDate?.let { end -> DateRow("Slutter", end, today) { onChange(draft.copy(endDate = it)) } }
}

@Composable
private fun SkipNextSection(draft: RiseAlarm, now: LocalDateTime, onChange: (RiseAlarm) -> Unit) {
    val next = draft.copy(enabled = true, skippedDates = emptySet()).nextOccurrence(now)?.date ?: return
    SettingsSection(null) {
        SwitchRow(
            title = "Hopp over neste gang",
            description = "Ringer ikke ${formatDate(next, now.date)}, men fortsetter som vanlig etterpå.",
            checked = next in draft.skippedDates,
            onCheckedChange = { skip ->
                onChange(draft.copy(skippedDates = if (skip) draft.skippedDates + next else draft.skippedDates - next))
            },
        )
    }
}

@Composable
private fun SoundSection(selected: String, onSelect: (RiseSound) -> Unit) {
    SettingsSection("Lyd") {
        RiseSound.entries.forEachIndexed { index, sound ->
            if (index > 0) SectionDivider()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onSelect(sound) },
            ) {
                Column(Modifier.weight(1f)) {
                    Text(sound.label, style = MaterialTheme.typography.bodyLarge)
                    Text(sound.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val isSelected = sound.id == selected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .then(
                            if (isSelected) {
                                Modifier.background(MaterialTheme.colorScheme.primary)
                            } else {
                                Modifier.border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            },
                        ),
                ) {
                    if (isSelected) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                }
            }
        }
        Hint("Trykk på en lyd for å høre den.")
    }
}

@Composable
private fun DateRow(label: String, date: LocalDate, today: LocalDate, onChange: (LocalDate) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        ChipButton(formatDate(date, today).replaceFirstChar { it.uppercase() }, selected = false) { picking = true }
    }
    if (picking) DatePickerPopup(date, onDismiss = { picking = false }, onPick = onChange)
}

@Composable
private fun Stepper(text: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepperButton("−", onMinus)
            Box(Modifier.width(1.dp).height(20.dp).background(MaterialTheme.colorScheme.outlineVariant))
            StepperButton("+", onPlus)
        }
    }
}

@Composable
private fun StepperButton(text: String, onClick: () -> Unit) {
    Text(
        text,
        fontSize = 22.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.clickable(onClick = onClick).width(48.dp).padding(vertical = 4.dp),
    )
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Warning(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerPopup(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    onPick(Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date)
                }
                onDismiss()
            }) { Text("Velg") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Avbryt") } },
    ) {
        DatePicker(state = state, showModeToggle = false)
    }
}
