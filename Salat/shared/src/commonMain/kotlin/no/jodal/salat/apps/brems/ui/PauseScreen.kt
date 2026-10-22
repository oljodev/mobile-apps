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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import no.jodal.salat.apps.brems.formatMinutes
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
    LaunchedEffect(remainingMillis == 0L) { if (remainingMillis == 0L) Haptics.success() }
    var step by remember { mutableStateOf(PauseStep.Waiting) }

    Box(Modifier.fillMaxSize().background(BremsPalette.Ink)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                PlainIconButton(Icons.Rounded.Close, "Gå ut av pausen", onLeave)
            }
            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(200)) },
                modifier = Modifier.weight(1f),
            ) { current ->
                when (current) {
                    PauseStep.Waiting -> Waiting(
                        store = store,
                        pause = pause,
                        now = now,
                        remainingMillis = remainingMillis,
                        onContinue = {
                            val known = appUrl(pause.app) != null
                            store.continueToApp()
                            if (known) onLeave() else step = PauseStep.GoBack
                        },
                        onStop = {
                            store.stop()
                            step = PauseStep.Stopped
                        },
                    )
                    PauseStep.Stopped -> Ending(
                        title = "Godt valgt.",
                        body = stoppedText(store),
                        onDone = onLeave,
                    )
                    PauseStep.GoBack -> Ending(
                        title = "Bytt tilbake.",
                        body = "Sveip tilbake til ${appName(pause.app)}. Om to minutter tar Brems en ny pause.",
                        onDone = onLeave,
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
    val name = appName(pause.app)
    val sinceStart = now - pause.startMillis
    val phase = (sinceStart % (BreathMillis * 2)).toFloat() / (BreathMillis * 2)
    val breathing = settings.breathing && !done

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.weight(0.7f))
        Orb(
            progress = remainingMillis.toFloat() / (pause.untilMillis - pause.startMillis).coerceAtLeast(1),
            breath = if (breathing) ((1 - cos(phase * 2 * PI)) / 2).toFloat() else 0.35f,
            seconds = ((remainingMillis + 999) / 1000).toInt(),
        )
        Spacer(Modifier.height(18.dp))
        Eyebrow(
            when {
                done -> "Pausen er over"
                breathing -> if (phase < 0.5f) "Pust inn" else "Pust ut"
                else -> "Vent litt"
            },
            color = BremsPalette.Ice,
        )
        Spacer(Modifier.weight(0.45f))

        Text(
            when {
                done -> "Vil du fortsatt inn?"
                pause.kind == PauseKind.Limit -> "Pust litt før $name."
                else -> "Et øyeblikk før $name."
            },
            style = MaterialTheme.typography.headlineSmall,
            color = BremsPalette.Text,
            textAlign = TextAlign.Center,
        )
        val details = buildList {
            if (pause.kind == PauseKind.Limit) add("Forrige økt varte over ${formatMinutes(settings.limitMinutes)}.")
            if (settings.showUsage) {
                val today = store.data.usageToday(now, TimeZone.currentSystemDefault())
                add("${today.opens}. gang i dag, ${formatDuration(today.millis)} så langt.")
            }
        }
        if (details.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(details.joinToString(" "), style = MaterialTheme.typography.bodyMedium, color = BremsPalette.TextDim, textAlign = TextAlign.Center)
        }
        val quote = pause.quote
        if (quote != null && !done) {
            Spacer(Modifier.height(28.dp))
            Text(
                quote,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                color = BremsPalette.Text.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        Spacer(Modifier.weight(1f))

        if (settings.askWhy && settings.reasons.isNotEmpty()) {
            val why = store.data.pauses.lastOrNull()?.why
            Text("Hvorfor åpner du $name?", style = MaterialTheme.typography.bodySmall, color = BremsPalette.TextDim)
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                settings.reasons.forEach { reason ->
                    Chip(reason, selected = why == reason) { store.answerWhy(if (why == reason) null else reason) }
                }
            }
            Spacer(Modifier.height(28.dp))
        }

        AnimatedContent(
            targetState = done,
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(200)) },
            modifier = Modifier.fillMaxWidth().height(122.dp),
        ) { finished ->
            if (finished) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton("Jeg er ferdig", onStop)
                    QuietButton("Fortsett til $name", onContinue)
                }
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        "Du kan gå ut av Brems. Pausen teller videre.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BremsPalette.TextFaint,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/**
 * Nedtellingen: en tynn ring som tømmes, og et lys inni som vokser og krymper med pusten.
 * [breath] går fra 0 (pustet ut) til 1 (pustet inn).
 */
@Composable
private fun Orb(progress: Float, breath: Float, seconds: Int) {
    val eased = FastOutSlowInEasing.transform(breath)
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(248.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 2.dp.toPx()
            val radius = size.minDimension / 2 - stroke
            // Lyset bak, størst når man har pustet inn.
            val glow = radius * (0.62f + 0.38f * eased)
            drawCircle(
                Brush.radialGradient(
                    0f to BremsPalette.IceDeep.copy(alpha = 0.55f),
                    0.6f to BremsPalette.IceDeep.copy(alpha = 0.18f),
                    1f to BremsPalette.IceDeep.copy(alpha = 0f),
                    center = center,
                    radius = glow,
                ),
                glow,
            )
            val disk = radius * (0.5f + 0.22f * eased)
            drawCircle(BremsPalette.Ice.copy(alpha = 0.06f + 0.06f * eased), disk)
            drawCircle(BremsPalette.Ice.copy(alpha = 0.18f + 0.22f * eased), disk, style = Stroke(1.dp.toPx()))
            // Ringen som tømmes.
            drawCircle(BremsPalette.Line, radius, style = Stroke(stroke))
            if (progress > 0f) {
                drawArc(
                    color = BremsPalette.Ice,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            "$seconds",
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 76.sp),
            color = BremsPalette.Text,
            modifier = Modifier.alpha(if (seconds == 0) 0.4f else 1f),
        )
    }
}

@Composable
private fun Ending(title: String, body: String, onDone: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.weight(1f))
        Text(title, style = MaterialTheme.typography.displaySmall, color = BremsPalette.Text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = BremsPalette.TextDim, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1.3f))
        PrimaryButton("Ferdig", onDone, Modifier.padding(bottom = 16.dp))
    }
}

private fun stoppedText(store: BremsStore): String {
    val stopped = store.data.usageToday(store.nowMillis(), TimeZone.currentSystemDefault()).stopped
    val times = if (stopped <= 1) "Første gang i dag." else "$stopped. gang i dag."
    return "Du lot være. $times\nLegg fra deg telefonen og gjør noe annet."
}
