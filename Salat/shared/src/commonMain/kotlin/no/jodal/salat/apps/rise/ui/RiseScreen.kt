package no.jodal.salat.apps.rise.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import no.jodal.salat.NativeBridge
import no.jodal.salat.apps.rise.AlarmKind
import no.jodal.salat.apps.rise.RiseAlarm
import no.jodal.salat.apps.rise.RiseGroup
import no.jodal.salat.apps.rise.Repeat
import no.jodal.salat.apps.rise.RiseSound
import no.jodal.salat.apps.rise.RiseStore
import no.jodal.salat.apps.rise.WakeUpGroupId
import no.jodal.salat.apps.rise.formatCountdown
import no.jodal.salat.apps.rise.formatDate
import no.jodal.salat.apps.rise.formatTime
import no.jodal.salat.apps.rise.nextOccurrence
import no.jodal.salat.apps.rise.summary
import no.jodal.salat.ui.LocalDarkTheme
import no.jodal.salat.ui.SalatColors

@Composable
fun RiseScreen(bridge: NativeBridge) {
    val darkTheme = LocalDarkTheme.current
    val store = remember { RiseStore.get(bridge) }
    var editing by remember { mutableStateOf<RiseAlarm?>(null) }
    var editingIsNew by remember { mutableStateOf(false) }
    var showGroups by remember { mutableStateOf(false) }
    val now by produceState(store.now()) {
        while (true) {
            delay(15_000)
            value = store.now()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AlarmList(
            store = store,
            now = now,
            darkTheme = darkTheme,
            onEdit = {
                editing = it
                editingIsNew = false
            },
            onShowGroups = { showGroups = true },
        )
        ExtendedFloatingActionButton(
            onClick = {
                editing = newAlarm(store, now)
                editingIsNew = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(WindowInsets.navigationBars.asPaddingValues())
                .padding(20.dp),
        ) {
            Text("+  Ny alarm", style = MaterialTheme.typography.titleMedium)
        }
    }

    editing?.let { alarm ->
        EditAlarmSheet(
            initial = alarm,
            isNew = editingIsNew,
            store = store,
            bridge = bridge,
            onDismiss = { editing = null },
        )
    }
    if (showGroups) {
        GroupsSheet(store = store, onDismiss = { showGroups = false })
    }
}

@Composable
private fun AlarmList(
    store: RiseStore,
    now: LocalDateTime,
    darkTheme: Boolean,
    onEdit: (RiseAlarm) -> Unit,
    onShowGroups: () -> Unit,
) {
    val data = store.data
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        store.pendingAwakeCheck?.let { title ->
            item(key = "awake") { AwakeCard(title, onConfirm = store::confirmAwake) }
        }
        store.scheduleError?.let { error ->
            item(key = "error") { ErrorCard(error) }
        }
        item(key = "next") { NextAlarmCard(data.alarms, data.groups, now, darkTheme) }
        item(key = "groups") {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = onShowGroups) { Text("Rediger grupper") }
            }
        }

        for (group in data.groups) {
            val alarms = data.alarms.filter { it.groupId == group.id }.sortedBy { it.hour * 60 + it.minute }
            item(key = "group-${group.id}") { GroupHeader(group, alarms.size) }
            if (alarms.isEmpty()) {
                item(key = "empty-${group.id}") { EmptyGroup() }
            }
            items(alarms, key = { it.id }) { alarm ->
                AlarmRow(
                    alarm = alarm,
                    now = now,
                    onClick = { onEdit(alarm) },
                    onToggle = { enabled ->
                        store.update { d -> d.copy(alarms = d.alarms.map { if (it.id == alarm.id) it.copy(enabled = enabled) else it }) }
                    },
                )
            }
        }
    }
}

@Composable
private fun NextAlarmCard(alarms: List<RiseAlarm>, groups: List<RiseGroup>, now: LocalDateTime, darkTheme: Boolean) {
    val next = alarms
        .mapNotNull { alarm -> alarm.nextOccurrence(now)?.let { alarm to it } }
        .minByOrNull { it.second }
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(SalatColors.Lemon, SalatColors.Orange).map { lerp(colors.surface, it, if (darkTheme) 0.38f else 0.32f) },
                ),
            )
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            if (next == null) "INGEN ALARMER ER PÅ" else "NESTE",
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 1.5.sp,
            color = colors.onSurfaceVariant,
        )
        if (next == null) {
            Text(
                "Trykk «Ny alarm» for å lage en.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 6.dp),
            )
            return@Column
        }
        val (alarm, at) = next
        val group = groups.firstOrNull { it.id == alarm.groupId }
        Text(formatTime(alarm.time), fontSize = 56.sp, fontWeight = FontWeight.Light, color = colors.onSurface)
        Text(
            listOfNotNull(
                formatDate(at.date, now.date).replaceFirstChar { it.uppercase() },
                alarm.title.ifBlank { null },
                group?.let { "${it.emoji} ${it.name}" },
            ).joinToString("  ·  "),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurface,
        )
        val zone = TimeZone.currentSystemDefault()
        Text(
            formatCountdown(at.toInstant(zone) - now.toInstant(zone)),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurface,
            modifier = Modifier
                .padding(top = 10.dp)
                .clip(CircleShape)
                .background(colors.surface.copy(alpha = 0.6f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun GroupHeader(group: RiseGroup, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp),
    ) {
        Text(group.emoji, fontSize = 20.sp)
        Text(group.name, style = MaterialTheme.typography.titleLarge)
        if (count > 0) {
            Text("$count", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyGroup() {
    Text(
        "Ingen alarmer her ennå",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f))
            .padding(horizontal = 18.dp, vertical = 16.dp),
    )
}

@Composable
private fun AlarmRow(alarm: RiseAlarm, now: LocalDateTime, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val dim = if (alarm.enabled) 1f else 0.45f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(start = 18.dp, end = 14.dp, top = 12.dp, bottom = 14.dp),
    ) {
        Column(Modifier.weight(1f).alpha(dim)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(formatTime(alarm.time), fontSize = 40.sp, fontWeight = FontWeight.Light, color = colors.onSurface)
                KindBadge(alarm.kind)
            }
            Text(
                alarm.title.ifBlank { if (alarm.kind == AlarmKind.Alarm) "Alarm" else "Påminnelse" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            val details = buildList {
                add(alarm.repeat.summary(now.date))
                val skipped = alarm.skippedDates.minOrNull()
                if (skipped != null) add("hopper over ${formatDate(skipped, now.date)}")
            }
            Text(details.joinToString("  ·  "), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        RiseSwitch(alarm.enabled, onToggle)
    }
}

@Composable
private fun KindBadge(kind: AlarmKind) {
    val (text, color) = when (kind) {
        AlarmKind.Alarm -> "⏰ Alarm" to SalatColors.Orange
        AlarmKind.Notification -> "🔔 Varsel" to SalatColors.Lavender
    }
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.25f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
fun AwakeCard(title: String, onConfirm: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SalatColors.Lemon)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("☀️ Er du oppe?", style = MaterialTheme.typography.titleLarge, color = OnLemon)
        Text(
            "«$title» ringer igjen om litt, helt til du bekrefter at du er oppe.",
            style = MaterialTheme.typography.bodyMedium,
            color = OnLemon,
        )
        Button(
            onClick = onConfirm,
            colors = ButtonDefaults.buttonColors(containerColor = OnLemon, contentColor = SalatColors.Lemon),
            modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
        ) {
            Text("Jeg er oppe!", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Text(
        message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(16.dp),
    )
}

private val OnLemon = Color(0xFF3A2E05)

/** En ny alarm med fornuftige standardverdier: kl. 07:00 i morgen. */
private fun newAlarm(store: RiseStore, now: LocalDateTime): RiseAlarm {
    val group = store.data.groups.first()
    val tomorrow = now.date.plus(DatePeriod(days = 1))
    return RiseAlarm(
        id = randomId(),
        title = "",
        groupId = group.id,
        kind = AlarmKind.Alarm,
        hour = 7,
        minute = 0,
        repeat = Repeat.Once(tomorrow),
        sound = RiseSound.Gentle.id,
        awakeCheck = group.id == WakeUpGroupId,
        startDate = now.date,
    )
}

fun randomId(): String = (1..12).map { "abcdefghijklmnopqrstuvwxyz0123456789".random() }.joinToString("")
