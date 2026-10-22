package no.jodal.salat.apps.brems.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlinx.datetime.TimeZone
import no.jodal.salat.Haptics
import no.jodal.salat.apps.brems.BremsStore
import no.jodal.salat.apps.brems.Pause
import no.jodal.salat.apps.brems.PauseKind
import no.jodal.salat.apps.brems.appName
import no.jodal.salat.apps.brems.appUrl
import no.jodal.salat.apps.brems.formatDuration
import no.jodal.salat.apps.brems.usageToday

/** Hvor man er etter at pausen er over. */
private enum class PauseStep { Waiting, Stopped, GoBack }

/** Pusten: fire sekunder inn, fire sekunder ut. */
private const val BreathMillis = 4_000L

/**
 * Pausen før en app åpnes. Den kan ikke hoppes over, men man kan gå ut av Brems: pausen teller
 * videre, og åpner man appen igjen, kommer man tilbake hit.
 */
@Composable
fun PauseScreen(store: BremsStore, pause: Pause, onLeave: () -> Unit) {
    var now by remember { mutableLongStateOf(store.nowMillis()) }
    LaunchedEffect(Unit) {
        val wallStart = store.nowMillis()
        var frameStart = -1L
        while (true) {
            withFrameMillis { frame ->
                if (frameStart < 0) frameStart = frame
                now = wallStart + (frame - frameStart)
            }
        }
    }
    val remainingMillis = (pause.untilMillis - now).coerceAtLeast(0)
    val done = remainingMillis == 0L
    var step by remember { mutableStateOf(PauseStep.Waiting) }
    LaunchedEffect(done) { if (done) Haptics.success() }

    val glow = BremsPalette.IceDeep
    Box(
        Modifier
            .fillMaxSize()
            .background(BremsPalette.Ink)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(glow.copy(alpha = 0.28f), glow.copy(alpha = 0f)),
                        center = Offset(size.width / 2, size.height * 0.34f),
                        radius = size.width * 0.9f,
                    ),
                )
            },
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                BremsLogo(28.dp)
                Text("Brems", style = MaterialTheme.typography.titleMedium, color = BremsPalette.TextDim, modifier = Modifier.padding(start = 10.dp).weight(1f))
                IconCircle(Icons.Rounded.Close, "Gå ut av pausen", onLeave)
            }
            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                modifier = Modifier.weight(1f),
            ) { current ->
                when (current) {
                    PauseStep.Waiting -> Waiting(store, pause, now, remainingMillis, onContinue = {
                        val known = appUrl(pause.app) != null
                        store.continueToApp()
                        if (known) onLeave() else step = PauseStep.GoBack
                    }, onStop = {
                        store.stop()
                        step = PauseStep.Stopped
                    })
                    PauseStep.Stopped -> Ending(
                        title = "Godt valgt.",
                        body = stoppedText(store),
                        button = "Ferdig",
                        onClick = onLeave,
                    )
                    PauseStep.GoBack -> Ending(
                        title = "Bytt tilbake.",
                        body = "Sveip tilbake til ${appName(pause.app)}. Du har to minutter før Brems tar en ny pause.",
                        button = "Ok",
                        onClick = onLeave,
                    )
                }
            }
        }
    }
}

@Composable
private fun Waiting(store: BremsStore, pause: Pause, now: Long, remainingMillis: Long, onContinue: () -> Unit, onStop: () -> Unit) {
    val settings = store.data.settings
    val done = remainingMillis == 0L
    val totalMillis = (pause.untilMillis - pause.startMillis).coerceAtLeast(1)
    val name = appName(pause.app)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.weight(0.6f))
        BreathingRing(
            progress = remainingMillis.toFloat() / totalMillis,
            breathing = settings.breathing && !done,
            sinceStart = now - pause.startMillis,
            seconds = ((remainingMillis + 999) / 1000).toInt(),
        )
        Spacer(Modifier.height(36.dp))
        Text(
            when {
                done -> "Pausen er over."
                pause.kind == PauseKind.Limit -> "Tid for en pause."
                else -> "Et lite øyeblikk."
            },
            style = MaterialTheme.typography.headlineMedium,
            color = BremsPalette.Text,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (pause.kind == PauseKind.Limit) {
                "Forrige økt gikk over ${settings.limitMinutes} minutter. Legg fra deg telefonen en stund."
            } else {
                "Du er på vei inn i $name."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = BremsPalette.TextDim,
            textAlign = TextAlign.Center,
        )
        if (settings.showUsage) {
            val today = store.data.usageToday(now, TimeZone.currentSystemDefault())
            Spacer(Modifier.height(20.dp))
            Text(
                "I dag  ·  ${formatDuration(today.millis)}  ·  ${today.opens} åpninger",
                style = MaterialTheme.typography.labelMedium,
                color = BremsPalette.Ice,
                modifier = Modifier.background(BremsPalette.Ice.copy(alpha = 0.1f), MaterialTheme.shapes.extraLarge).padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
        val quote = pause.quote
        if (quote != null) {
            Spacer(Modifier.height(24.dp))
            Text(
                "«$quote»",
                style = MaterialTheme.typography.bodyLarge,
                color = BremsPalette.Text.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        if (settings.askWhy && settings.reasons.isNotEmpty()) {
            Text("Hvorfor åpner du $name?", style = MaterialTheme.typography.titleSmall, color = BremsPalette.TextDim)
            Spacer(Modifier.height(12.dp))
            val why = store.data.pauses.lastOrNull()?.why
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                settings.reasons.forEach { reason ->
                    Chip(reason, selected = why == reason) { store.answerWhy(if (why == reason) null else reason) }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        AnimatedContent(targetState = done, transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(200)) }) { finished ->
            if (finished) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                    SecondaryButton("Fortsett til $name", onContinue)
                    PrimaryButton("Nei, jeg er ferdig", onStop)
                }
            } else {
                Text(
                    "Pausen teller videre selv om du går ut av Brems.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BremsPalette.TextFaint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().height(122.dp).padding(top = 40.dp),
                )
            }
        }
    }
}

/** En ring som teller ned, med en sirkel inni som vokser og krymper i takt med pusten. */
@Composable
private fun BreathingRing(progress: Float, breathing: Boolean, sinceStart: Long, seconds: Int) {
    val phase = (sinceStart % (BreathMillis * 2)).toFloat() / (BreathMillis * 2)
    // 0 → 1 → 0 over én pust, mykt i endene.
    val breath = if (breathing) ((1 - cos(phase * 2 * PI)) / 2).toFloat() else 0.5f
    val inhaling = phase < 0.5f
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 6.dp.toPx()
            val radius = size.minDimension / 2 - stroke
            drawCircle(BremsPalette.Line, radius, style = Stroke(stroke))
            drawArc(
                brush = Brush.sweepGradient(listOf(BremsPalette.IceDeep, BremsPalette.Ice, BremsPalette.IceDeep)),
                startAngle = -90f,
                sweepAngle = -360f * progress,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            val inner = radius * (0.48f + 0.36f * FastOutSlowInEasing.transform(breath))
            drawCircle(
                Brush.radialGradient(
                    listOf(BremsPalette.Ice.copy(alpha = 0.26f), BremsPalette.Ice.copy(alpha = 0.04f)),
                    center = center,
                    radius = inner,
                ),
                inner,
            )
            drawCircle(BremsPalette.Ice.copy(alpha = 0.35f), inner, style = Stroke(1.dp.toPx()))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (seconds > 0) "$seconds" else "0",
                style = MaterialTheme.typography.displayLarge,
                color = BremsPalette.Text,
            )
            if (breathing) {
                Text(if (inhaling) "Pust inn" else "Pust ut", style = MaterialTheme.typography.labelMedium, color = BremsPalette.TextDim)
            }
        }
    }
}

@Composable
private fun Ending(title: String, body: String, button: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.weight(1f))
        Text(title, style = MaterialTheme.typography.displaySmall, color = BremsPalette.Text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = BremsPalette.TextDim, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        PrimaryButton(button, onClick, Modifier.padding(bottom = 16.dp))
    }
}

private fun stoppedText(store: BremsStore): String {
    val stopped = store.data.usageToday(store.nowMillis(), TimeZone.currentSystemDefault()).stopped
    val times = if (stopped == 1) "Første gang i dag" else "$stopped ganger i dag"
    return "Du lot være. $times. Legg fra deg telefonen og gjør noe annet."
}
