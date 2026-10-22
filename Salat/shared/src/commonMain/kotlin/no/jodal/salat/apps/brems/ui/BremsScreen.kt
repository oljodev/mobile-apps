package no.jodal.salat.apps.brems.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import no.jodal.salat.DeepLinks
import no.jodal.salat.NativeBridge
import no.jodal.salat.apps.brems.BremsData
import no.jodal.salat.apps.brems.BremsSettings
import no.jodal.salat.apps.brems.BremsStore
import no.jodal.salat.apps.brems.DayUsage
import no.jodal.salat.apps.brems.appName
import no.jodal.salat.apps.brems.dayFraction
import no.jodal.salat.apps.brems.formatDuration
import no.jodal.salat.apps.brems.segmentsOn
import no.jodal.salat.apps.brems.today
import no.jodal.salat.apps.brems.usageToday
import no.jodal.salat.ui.BackButton
import no.jodal.salat.ui.pressable

private enum class BremsDestination { Home, Settings, Guide, Pause }

@Composable
fun BremsScreen(bridge: NativeBridge, onBack: () -> Unit) {
    BremsTheme {
        val store = remember { BremsStore.get(bridge) }
        var destination by remember {
            mutableStateOf(if (store.currentPause() != null) BremsDestination.Pause else BremsDestination.Home)
        }
        // Snarveier åpner Brems med salat://brems/pause når en app skal vente.
        val link = DeepLinks.pending
        LaunchedEffect(link) {
            if (DeepLinks.consume("brems", "pause") && store.currentPause() != null) destination = BremsDestination.Pause
        }

        Box(Modifier.fillMaxSize().background(BremsPalette.Ink)) {
            AnimatedContent(
                targetState = destination,
                transitionSpec = {
                    val duration = 280
                    when {
                        targetState == BremsDestination.Pause ->
                            slideInVertically(tween(duration)) { it / 6 } + fadeIn(tween(duration)) togetherWith fadeOut(tween(duration))
                        initialState == BremsDestination.Pause ->
                            fadeIn(tween(duration)) togetherWith slideOutVertically(tween(duration)) { it / 6 } + fadeOut(tween(duration))
                        targetState == BremsDestination.Home ->
                            slideInHorizontally(tween(duration)) { -it / 4 } + fadeIn(tween(duration)) togetherWith slideOutHorizontally(tween(duration)) { it }
                        else ->
                            slideInHorizontally(tween(duration)) { it } togetherWith slideOutHorizontally(tween(duration)) { -it / 4 } + fadeOut(tween(duration))
                    }
                },
            ) { current ->
                when (current) {
                    BremsDestination.Home -> Home(store, onBack = onBack, onOpen = { destination = it })
                    BremsDestination.Settings -> SettingsScreen(store, onBack = { destination = BremsDestination.Home })
                    BremsDestination.Guide -> GuideScreen(bridge, store, onBack = { destination = BremsDestination.Home })
                    BremsDestination.Pause -> {
                        val pause = remember { store.currentPause() ?: store.data.pauses.lastOrNull() }
                        if (pause == null) {
                            LaunchedEffect(Unit) { destination = BremsDestination.Home }
                        } else {
                            PauseScreen(store, pause, onLeave = { destination = BremsDestination.Home })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Home(store: BremsStore, onBack: () -> Unit, onOpen: (BremsDestination) -> Unit) {
    // Forsiden oppdateres hvert sekund, så nedtellingen og tiden i appen som er åpen går.
    var now by remember { mutableLongStateOf(store.nowMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = store.nowMillis()
        }
    }
    val data = store.data
    val zone = TimeZone.currentSystemDefault()
    val today = data.usageToday(now, zone)
    val pause = store.currentPause()?.takeIf { now < it.untilMillis }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Box(Modifier.weight(1f).padding(start = 0.dp)) { BackButton(onBack, color = BremsPalette.Ice) }
            PlainIconButton(Icons.Rounded.Tune, "Innstillinger", { onOpen(BremsDestination.Settings) })
        }

        Spacer(Modifier.height(28.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BremsLogo(22.dp)
            Eyebrow(if (data.settings.enabled) "Brems  ·  i dag" else "Brems er av")
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            val minutes = today.millis / 60_000
            Text(
                if (minutes < 60) "$minutes" else "${minutes / 60}:${(minutes % 60).toString().padStart(2, '0')}",
                style = MaterialTheme.typography.displayLarge,
                color = BremsPalette.Text,
            )
            Text(
                if (minutes < 60) "min" else "timer",
                style = MaterialTheme.typography.titleLarge,
                color = BremsPalette.TextDim,
                modifier = Modifier.padding(start = 8.dp, bottom = 16.dp),
            )
        }
        Text("i appene du bremser", style = MaterialTheme.typography.bodyMedium, color = BremsPalette.TextDim)

        Spacer(Modifier.height(28.dp))
        DayTimeline(data, now, zone)

        val active = data.active
        if (pause != null || active != null) {
            Spacer(Modifier.height(24.dp))
            if (pause != null) {
                LiveRow(
                    text = "Pause før ${appName(pause.app)}",
                    value = "${(pause.untilMillis - now + 999) / 1000} s",
                    onClick = { onOpen(BremsDestination.Pause) },
                )
            } else if (active != null) {
                LiveRow(text = "${appName(active.app).replaceFirstChar { it.uppercase() }} er åpen", value = formatDuration(now - active.startMillis))
            }
        }

        Spacer(Modifier.height(32.dp))
        Stats(today)

        if (data.opens.isEmpty()) {
            Spacer(Modifier.height(32.dp))
            SetupCard { onOpen(BremsDestination.Guide) }
        }

        Spacer(Modifier.height(40.dp))
        SectionHeader("Regler", action = "Endre", onAction = { onOpen(BremsDestination.Settings) })
        Spacer(Modifier.height(14.dp))
        Rules(data.settings) { onOpen(BremsDestination.Settings) }

        if (today.apps.any { it.first.isNotEmpty() }) {
            Spacer(Modifier.height(40.dp))
            SectionHeader("Apper i dag")
            Spacer(Modifier.height(18.dp))
            Apps(today)
        }

        Spacer(Modifier.height(40.dp))
        BremsCard(onClick = { onOpen(BremsDestination.Guide) }, padding = 0.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Oppsett og logg", style = MaterialTheme.typography.titleMedium, color = BremsPalette.Text)
                    Text("Automasjonene i Snarveier", style = MaterialTheme.typography.bodySmall, color = BremsPalette.TextDim)
                }
                Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = null, tint = BremsPalette.TextFaint, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

/** Døgnet som en stripe, med de gangene appene var åpne lyst opp. */
@Composable
private fun DayTimeline(data: BremsData, now: Long, zone: TimeZone) {
    val segments = data.segmentsOn(today(now, zone), now, zone)
    val nowFraction = dayFraction(now, zone)
    Column {
        Canvas(Modifier.fillMaxWidth().height(28.dp)) {
            val bar = size.height
            drawRoundRect(BremsPalette.Surface, cornerRadius = CornerRadius(8.dp.toPx()))
            segments.forEach { range ->
                val x = range.start * size.width
                val w = ((range.endInclusive - range.start) * size.width).coerceAtLeast(3.dp.toPx())
                drawRoundRect(BremsPalette.Ice, topLeft = Offset(x, 0f), size = Size(w, bar), cornerRadius = CornerRadius(3.dp.toPx()))
            }
            // Nå: en tynn strek, så man ser hvor langt dagen har kommet.
            val x = nowFraction * size.width
            drawLine(BremsPalette.TextDim, Offset(x, -4.dp.toPx()), Offset(x, bar + 4.dp.toPx()), strokeWidth = 1.5f.dp.toPx())
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("00", "06", "12", "18", "24").forEach {
                Text(it, style = MaterialTheme.typography.labelMedium, color = BremsPalette.TextFaint)
            }
        }
    }
}

/** Det som skjer akkurat nå, med en lysende prikk. */
@Composable
private fun LiveRow(text: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(BremsPalette.Ice.copy(alpha = 0.09f))
            .then(if (onClick != null) Modifier.pressable(onClick, pressedScale = 0.98f) else Modifier)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(BremsPalette.Ice))
        Text(text, style = MaterialTheme.typography.titleSmall, color = BremsPalette.Text, modifier = Modifier.padding(start = 12.dp).weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall, color = BremsPalette.Ice)
    }
}

@Composable
private fun Stats(today: DayUsage) {
    Row(Modifier.fillMaxWidth()) {
        Stat("${today.opens}", "åpninger", Modifier.weight(1f))
        Box(Modifier.width(1.dp).height(44.dp).background(BremsPalette.Line))
        Stat("${today.pauses}", "pauser", Modifier.weight(1f).padding(start = 20.dp))
        Box(Modifier.width(1.dp).height(44.dp).background(BremsPalette.Line))
        Stat("${today.stopped}", "lot være", Modifier.weight(1f).padding(start = 20.dp))
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = BremsPalette.Text)
        Text(label, style = MaterialTheme.typography.bodySmall, color = BremsPalette.TextDim)
    }
}

@Composable
private fun SetupCard(onClick: () -> Unit) {
    BremsCard(onClick = onClick) {
        Eyebrow("Kom i gang", color = BremsPalette.Ice)
        Spacer(Modifier.height(10.dp))
        Text("Koble Brems til Snarveier", style = MaterialTheme.typography.titleLarge, color = BremsPalette.Text)
        Spacer(Modifier.height(6.dp))
        Text(
            "To automasjoner forteller Brems når du åpner og lukker apper. Det tar et par minutter.",
            style = MaterialTheme.typography.bodyMedium,
            color = BremsPalette.TextDim,
        )
    }
}

/** Reglene som fire ruter med store tall. */
@Composable
private fun Rules(settings: BremsSettings, onClick: () -> Unit) {
    fun seconds(value: Int) = if (value == 0) "Av" else if (value < 60 || value % 60 != 0) "$value s" else "${value / 60} min"
    fun minutes(value: Int) = if (value == 0) "Av" else "$value min"
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RuleTile(seconds(settings.openPauseSeconds), "pause når du åpner", Modifier.weight(1f), onClick)
            RuleTile(minutes(settings.reminderMinutes), "før første varsel", Modifier.weight(1f), onClick)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RuleTile(minutes(settings.limitMinutes), "grense per økt", Modifier.weight(1f), onClick)
            RuleTile(seconds(settings.limitPauseSeconds), "pause etter grensen", Modifier.weight(1f), onClick)
        }
    }
}

@Composable
private fun RuleTile(value: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    BremsCard(modifier, onClick = onClick, padding = 18.dp) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = BremsPalette.Ice)
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = BremsPalette.TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Apps(today: DayUsage) {
    val max = today.apps.maxOf { it.second }.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        today.apps.forEach { (app, millis) ->
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        app.ifEmpty { "Andre apper" },
                        style = MaterialTheme.typography.titleMedium,
                        color = BremsPalette.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(formatDuration(millis), style = MaterialTheme.typography.labelMedium, color = BremsPalette.TextDim)
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(BremsPalette.Surface)) {
                    Box(Modifier.fillMaxWidth(millis.toFloat() / max).fillMaxHeight().clip(CircleShape).background(BremsPalette.Ice))
                }
            }
        }
    }
}
