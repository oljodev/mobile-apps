package no.jodal.salat.apps.rise.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import no.jodal.salat.NativeBridge
import no.jodal.salat.apps.rise.AlarmKind
import no.jodal.salat.apps.rise.Repeat
import no.jodal.salat.apps.rise.RiseAlarm
import no.jodal.salat.apps.rise.RiseGroup
import no.jodal.salat.apps.rise.RiseSound
import no.jodal.salat.apps.rise.RiseStore
import no.jodal.salat.apps.rise.formatCountdown
import no.jodal.salat.apps.rise.formatDate
import no.jodal.salat.apps.rise.formatTime
import no.jodal.salat.apps.rise.nextOccurrence
import no.jodal.salat.apps.rise.summary
import no.jodal.salat.resources.Res
import no.jodal.salat.resources.rise_logo
import no.jodal.salat.ui.BackButton
import no.jodal.salat.ui.LocalDarkTheme
import org.jetbrains.compose.resources.painterResource

@Composable
fun RiseScreen(bridge: NativeBridge, onBack: () -> Unit) {
    RiseTheme {
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
        val listState = rememberLazyListState()
        val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 120 } }

        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 32.dp),
            ) {
                item(key = "hero") { Hero(store.data.alarms, store.data.groups, now) }

                store.scheduleError?.let { error ->
                    item(key = "error") { ErrorCard(error) }
                }

                for (group in store.data.groups) {
                    val alarms = store.data.alarms.filter { it.groupId == group.id }.sortedBy { it.hour * 60 + it.minute }
                    item(key = "group-${group.id}") {
                        GroupSection(
                            group = group,
                            alarms = alarms,
                            now = now,
                            onEdit = {
                                editing = it
                                editingIsNew = false
                            },
                            onToggle = { alarm, enabled ->
                                store.update { d -> d.copy(alarms = d.alarms.map { if (it.id == alarm.id) it.copy(enabled = enabled) else it }) }
                            },
                        )
                    }
                }
            }

            TopBar(
                scrolled = scrolled,
                onBack = onBack,
                onGroups = { showGroups = true },
                onAdd = {
                    editing = newAlarm(store, now)
                    editingIsNew = true
                },
            )
        }

        editing?.let { alarm ->
            EditAlarmSheet(initial = alarm, isNew = editingIsNew, store = store, bridge = bridge, onDismiss = { editing = null })
        }
        if (showGroups) {
            GroupsSheet(store = store, onDismiss = { showGroups = false })
        }
    }
}

/** Topplinjen ligger oppå soloppgangen, og får bakgrunn når man scroller. */
@Composable
private fun TopBar(scrolled: Boolean, onBack: () -> Unit, onGroups: () -> Unit, onAdd: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(if (scrolled) colors.background else Color.Transparent)
    val content by animateColorAsState(if (scrolled) colors.onBackground else RisePalette.White)
    val accent by animateColorAsState(if (scrolled) colors.primary else RisePalette.White)

    Box(Modifier.fillMaxWidth().background(background).statusBarsPadding().height(56.dp)) {
        Box(Modifier.align(Alignment.CenterStart).padding(start = 4.dp)) {
            BackButton(onBack, color = accent)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Image(painterResource(Res.drawable.rise_logo), contentDescription = null, modifier = Modifier.size(28.dp))
            Text("Rise", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = content)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
        ) {
            RoundIconButton(Icons.Rounded.Tune, "Grupper", background = content.copy(alpha = 0.14f), tint = content, onClick = onGroups)
            RoundIconButton(
                Icons.Rounded.Add,
                "Ny alarm",
                background = if (scrolled) colors.primary else RisePalette.White,
                tint = if (scrolled) colors.onPrimary else Color(0xFF6B3F86),
                onClick = onAdd,
            )
        }
    }
}

@Composable
private fun RoundIconButton(icon: ImageVector, description: String, background: Color, tint: Color, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(36.dp).clip(CircleShape).background(background).clickable(onClick = onClick),
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** Soloppgangen øverst, med neste alarm. */
@Composable
private fun Hero(alarms: List<RiseAlarm>, groups: List<RiseGroup>, now: LocalDateTime) {
    val darkTheme = LocalDarkTheme.current
    val next = alarms
        .mapNotNull { alarm -> alarm.nextOccurrence(now)?.let { alarm to it } }
        .minByOrNull { it.second }
    val white = RisePalette.White

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(RisePalette.sunrise(darkTheme)),
    ) {
        SunGlow(Modifier.matchParentSize())
        Column(
            modifier = Modifier
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp)
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 32.dp),
        ) {
            Text(
                if (next == null) "INGEN ALARMER ER PÅ" else "NESTE ALARM",
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 2.sp,
                color = white.copy(alpha = 0.75f),
            )
            if (next == null) {
                Text(
                    "Trykk + for å lage din første.",
                    style = MaterialTheme.typography.titleMedium,
                    color = white,
                    modifier = Modifier.padding(top = 10.dp, bottom = 24.dp),
                )
                return@Column
            }
            val (alarm, at) = next
            Text(formatTime(alarm.time), fontSize = 84.sp, fontWeight = FontWeight.ExtraLight, color = white)
            Text(
                listOfNotNull(
                    formatDate(at.date, now.date).replaceFirstChar { it.uppercase() },
                    alarm.title.ifBlank { null },
                    groups.firstOrNull { it.id == alarm.groupId }?.name,
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
                color = white.copy(alpha = 0.92f),
            )
            val zone = TimeZone.currentSystemDefault()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clip(CircleShape)
                    .background(white.copy(alpha = 0.18f))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Icon(Icons.Rounded.Schedule, contentDescription = null, tint = white, modifier = Modifier.size(16.dp))
                Text(
                    formatCountdown(at.toInstant(zone) - now.toInstant(zone)).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelLarge,
                    color = white,
                )
            }
        }
    }
}

/** En sol som stiger opp bak horisonten nederst til høyre. */
@Composable
private fun SunGlow(modifier: Modifier) {
    Canvas(modifier) {
        val center = Offset(size.width * 0.82f, size.height * 1.02f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(RisePalette.Gold.copy(alpha = 0.55f), RisePalette.Gold.copy(alpha = 0.15f), Color.Transparent),
                center = center,
                radius = size.width * 0.75f,
            ),
            radius = size.width * 0.75f,
            center = center,
        )
        drawCircle(color = RisePalette.Gold.copy(alpha = 0.9f), radius = size.width * 0.16f, center = center)
    }
}

@Composable
private fun GroupSection(
    group: RiseGroup,
    alarms: List<RiseAlarm>,
    now: LocalDateTime,
    onEdit: (RiseAlarm) -> Unit,
    onToggle: (RiseAlarm, Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)) {
            Text(group.name.uppercase(), style = MaterialTheme.typography.labelLarge, letterSpacing = 1.5.sp, color = colors.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            if (alarms.isNotEmpty()) {
                val active = alarms.count { it.enabled }
                Text("$active av ${alarms.size} på", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.surfaceContainer)) {
            if (alarms.isEmpty()) {
                Text(
                    "Ingen alarmer her ennå",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                )
            }
            alarms.forEachIndexed { index, alarm ->
                if (index > 0) HorizontalDivider(color = colors.outlineVariant, modifier = Modifier.padding(start = 20.dp))
                AlarmRow(alarm, now, onClick = { onEdit(alarm) }, onToggle = { onToggle(alarm, it) })
            }
        }
    }
}

@Composable
private fun AlarmRow(alarm: RiseAlarm, now: LocalDateTime, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 16.dp, top = 10.dp, bottom = 14.dp),
    ) {
        Column(Modifier.weight(1f).alpha(if (alarm.enabled) 1f else 0.4f)) {
            Text(formatTime(alarm.time), fontSize = 42.sp, fontWeight = FontWeight.Light, color = colors.onSurface)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    if (alarm.kind == AlarmKind.Alarm) Icons.Rounded.Alarm else Icons.Rounded.NotificationsNone,
                    contentDescription = if (alarm.kind == AlarmKind.Alarm) "Alarm" else "Varsel",
                    tint = colors.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    alarm.title.ifBlank { if (alarm.kind == AlarmKind.Alarm) "Alarm" else "Påminnelse" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                )
            }
            val details = buildList {
                add(alarm.repeat.summary(now.date))
                alarm.skippedDates.minOrNull()?.let { add("hopper over ${formatDate(it, now.date)}") }
            }
            Text(
                details.joinToString("  ·  "),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        RiseSwitch(alarm.enabled, onToggle)
    }
}

@Composable
private fun ErrorCard(message: String) {
    Text(
        message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(16.dp),
    )
}

/** En ny alarm med fornuftige standardverdier: kl. 07:00 i morgen. */
private fun newAlarm(store: RiseStore, now: LocalDateTime): RiseAlarm {
    val group = store.data.groups.first()
    return RiseAlarm(
        id = randomId(),
        title = "",
        groupId = group.id,
        kind = AlarmKind.Alarm,
        hour = 7,
        minute = 0,
        repeat = Repeat.Once(now.date.plus(DatePeriod(days = 1))),
        sound = RiseSound.Gentle.id,
        startDate = now.date,
    )
}

fun randomId(): String = (1..12).map { "abcdefghijklmnopqrstuvwxyz0123456789".random() }.joinToString("")
