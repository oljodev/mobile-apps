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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import no.jodal.salat.DeepLinks
import no.jodal.salat.NativeBridge
import no.jodal.salat.apps.brems.BremsSettings
import no.jodal.salat.apps.brems.BremsStore
import no.jodal.salat.apps.brems.DayUsage
import no.jodal.salat.apps.brems.appName
import no.jodal.salat.apps.brems.formatDuration
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
                    BremsDestination.Home -> Home(
                        store = store,
                        onBack = onBack,
                        onOpen = { destination = it },
                    )
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
    // Klokka på forsiden går i takt med minuttene og nedtellingen.
    var now by remember { mutableLongStateOf(store.nowMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = store.nowMillis()
        }
    }
    val data = store.data
    val today = data.usageToday(now, TimeZone.currentSystemDefault())
    val pause = store.currentPause()?.takeIf { now < it.untilMillis }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Box(Modifier.weight(1f)) { BackButton(onBack, color = BremsPalette.Ice) }
            IconCircle(Icons.Rounded.Settings, "Innstillinger", { onOpen(BremsDestination.Settings) })
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BremsLogo(52.dp)
            Column {
                Text("Brems", style = MaterialTheme.typography.headlineMedium, color = BremsPalette.Text)
                Text(
                    if (data.settings.enabled) "Passer på mens du scroller" else "Skrudd av",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BremsPalette.TextDim,
                )
            }
        }

        if (pause != null) {
            BremsCard(onClick = { onOpen(BremsDestination.Pause) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Pause pågår", style = MaterialTheme.typography.titleMedium, color = BremsPalette.Text)
                        Text("Før ${appName(pause.app)}", style = MaterialTheme.typography.bodySmall, color = BremsPalette.TextDim)
                    }
                    Text("${(pause.untilMillis - now + 999) / 1000} s", style = MaterialTheme.typography.headlineSmall, color = BremsPalette.Ice)
                }
            }
        }

        TodayCard(today, store, now)

        if (data.opens.isEmpty()) {
            SetupCard { onOpen(BremsDestination.Guide) }
        }

        SectionLabel("Reglene dine", Modifier.padding(top = 8.dp))
        RulesCard(data.settings) { onOpen(BremsDestination.Settings) }

        if (today.apps.size > 1 || today.apps.singleOrNull()?.first?.isNotEmpty() == true) {
            SectionLabel("I dag per app", Modifier.padding(top = 8.dp))
            AppsCard(today)
        }

        SectionLabel("Oppsett", Modifier.padding(top = 8.dp))
        BremsCard(onClick = { onOpen(BremsDestination.Guide) }, padding = 0.dp) {
            LinkRow("Automasjoner i Snarveier", "Slik får Brems vite når du åpner apper")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TodayCard(today: DayUsage, store: BremsStore, now: Long) {
    BremsCard {
        SectionLabel("I dag", Modifier.padding(start = 0.dp))
        Spacer(Modifier.height(10.dp))
        Text(formatDuration(today.millis), style = MaterialTheme.typography.displayMedium, color = BremsPalette.Text)
        Text("i appene Brems følger med på", style = MaterialTheme.typography.bodyMedium, color = BremsPalette.TextDim)
        val active = store.data.active
        if (active != null) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(BremsPalette.Ice))
                Text(
                    "${appName(active.app).replaceFirstChar { it.uppercase() }} er åpen  ·  ${formatDuration(now - active.startMillis)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = BremsPalette.Ice,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth()) {
            Stat("${today.opens}", "Åpninger", Modifier.weight(1f))
            Stat("${today.pauses}", "Pauser", Modifier.weight(1f))
            Stat("${today.stopped}", "Lot være", Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = BremsPalette.Text)
        Text(label, style = MaterialTheme.typography.bodySmall, color = BremsPalette.TextDim)
    }
}

@Composable
private fun SetupCard(onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick, pressedScale = 0.98f)
            .clip(shape)
            .background(BremsPalette.Ice.copy(alpha = 0.08f))
            .border(1.dp, BremsPalette.Ice.copy(alpha = 0.5f), shape)
            .padding(20.dp),
    ) {
        Text("Kom i gang", style = MaterialTheme.typography.titleLarge, color = BremsPalette.Ice)
        Spacer(Modifier.height(6.dp))
        Text(
            "Brems trenger to automasjoner i Snarveier for å vite når du åpner og lukker apper. Det tar et par minutter.",
            style = MaterialTheme.typography.bodyMedium,
            color = BremsPalette.Text,
        )
        Spacer(Modifier.height(16.dp))
        Text("Vis meg hvordan", style = MaterialTheme.typography.labelLarge, color = BremsPalette.Ice)
    }
}

@Composable
private fun RulesCard(settings: BremsSettings, onClick: () -> Unit) {
    BremsCard(onClick = onClick, padding = 0.dp) {
        RuleRow("Pause når du åpner en app", if (settings.openPauseSeconds == 0) "Av" else "${settings.openPauseSeconds} s")
        Divider()
        RuleRow("Varsel etter", if (settings.reminderMinutes == 0) "Av" else "${settings.reminderMinutes} min")
        Divider()
        RuleRow("Grense per økt", "${settings.limitMinutes} min")
        Divider()
        RuleRow("Pause etter grensen", "${settings.limitPauseSeconds} s")
    }
}

@Composable
private fun RuleRow(title: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = BremsPalette.Text, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = BremsPalette.Ice)
    }
}

@Composable
private fun AppsCard(today: DayUsage) {
    val max = today.apps.maxOf { it.second }.coerceAtLeast(1)
    BremsCard {
        today.apps.forEachIndexed { index, (app, millis) ->
            if (index > 0) Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    app.ifEmpty { "Andre apper" },
                    style = MaterialTheme.typography.titleSmall,
                    color = BremsPalette.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(formatDuration(millis), style = MaterialTheme.typography.labelMedium, color = BremsPalette.TextDim)
            }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(BremsPalette.SurfaceHigh)) {
                Box(Modifier.fillMaxWidth(millis.toFloat() / max).fillMaxHeight().clip(CircleShape).background(BremsPalette.Ice))
            }
        }
    }
}

@Composable
fun LinkRow(title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = BremsPalette.Text)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = BremsPalette.TextDim)
        }
        Spacer(Modifier.width(12.dp))
        Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = null, tint = BremsPalette.TextFaint, modifier = Modifier.size(14.dp))
    }
}
