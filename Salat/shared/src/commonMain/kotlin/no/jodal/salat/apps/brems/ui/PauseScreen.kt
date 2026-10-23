package no.jodal.salat.apps.brems.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
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
import no.jodal.salat.ui.pressable

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
    LaunchedEffect(done) { if (done) Haptics.success() }
    var step by remember { mutableStateOf(PauseStep.Waiting) }

    val phase = ((now - pause.startMillis) % (BreathMillis * 2)).toFloat() / (BreathMillis * 2)
    val breathing = store.data.settings.breathing && !done && step == PauseStep.Waiting
    // 0 når man har pustet ut, 1 når man har pustet inn.
    val breath = if (breathing) ((1 - cos(phase * 2 * PI)) / 2).toFloat() else 0.45f

    DuskBackground(breath) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                AppPill(appName(pause.app).replaceFirstChar { it.uppercase() })
                Spacer(Modifier.weight(1f))
                GlassIconButton(Icons.Rounded.Close, "Gå ut av pausen", onLeave)
            }
            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(250)) },
                modifier = Modifier.weight(1f),
            ) { current ->
                when (current) {
                    PauseStep.Waiting -> Waiting(
                        store = store,
                        pause = pause,
                        now = now,
                        remainingMillis = remainingMillis,
                        breath = breath,
                        inhaling = breathing && phase < 0.5f,
                        breathing = breathing,
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
                    PauseStep.Stopped -> Ending("Godt valgt.", stoppedText(store), onLeave)
                    PauseStep.GoBack -> Ending(
                        "Bytt tilbake.",
                        "Sveip tilbake til ${appName(pause.app)}. Om to minutter tar Brems en ny pause.",
                        onLeave,
                    )
                }
            }
        }
    }
}

@Composable
private fun Waiting(
    store: BremsStore,
    pause: Pause,
    now: Long,
    remainingMillis: Long,
    breath: Float,
    inhaling: Boolean,
    breathing: Boolean,
    onContinue: () -> Unit,
    onStop: () -> Unit,
) {
    val settings = store.data.settings
    val done = remainingMillis == 0L
    val name = appName(pause.app)
    val limit = pause.kind == PauseKind.Limit

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(28.dp))
        Text(
            if (limit) "OVER GRENSEN" else "PAUSE",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.4.sp),
            color = if (limit) Dusk.Ember else Dusk.Lilac,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            when {
                done -> "Vil du fortsatt inn?"
                limit -> "Pust litt før $name."
                else -> "Et øyeblikk før $name."
            },
            style = MaterialTheme.typography.headlineSmall,
            color = Dusk.Text,
            textAlign = TextAlign.Center,
        )
        val details = buildList {
            if (limit) add("Forrige økt varte over ${formatMinutes(settings.limitMinutes)}.")
            if (settings.showUsage) {
                val today = store.data.usageToday(now, TimeZone.currentSystemDefault())
                add("${today.opens}. gang i dag  ·  ${formatDuration(today.millis)}")
            }
        }
        if (details.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(details.joinToString("\n"), style = MaterialTheme.typography.bodyMedium, color = Dusk.TextDim, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.weight(0.6f))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(292.dp)) {
            Bloom(
                breath = breath,
                progress = remainingMillis.toFloat() / (pause.untilMillis - pause.startMillis).coerceAtLeast(1),
            )
            Crossfade(targetState = done, animationSpec = tween(500)) { finished ->
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                    if (finished) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Dusk.Text, modifier = Modifier.size(64.dp))
                    } else {
                        Text(
                            "${(remainingMillis + 999) / 1000}",
                            style = MaterialTheme.typography.displayLarge,
                            color = Dusk.Text,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Crossfade(targetState = if (breathing) (if (inhaling) "Pust inn" else "Pust ut") else "", animationSpec = tween(700)) { cue ->
            Text(
                cue,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Light, fontSize = 22.sp),
                color = Dusk.TextDim,
                modifier = Modifier.height(30.dp),
            )
        }

        val quote = pause.quote
        Spacer(Modifier.weight(0.5f))
        if (quote != null && !done) {
            Text(
                quote,
                style = MaterialTheme.typography.bodyLarge,
                color = Dusk.TextDim,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 300.dp),
            )
        }
        Spacer(Modifier.weight(1f))

        if (settings.askWhy && settings.reasons.isNotEmpty()) {
            val why = store.data.pauses.lastOrNull()?.why
            Text("Hvorfor åpner du $name?", style = MaterialTheme.typography.bodySmall, color = Dusk.TextFaint)
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                settings.reasons.forEach { reason ->
                    GlassChip(reason, selected = why == reason) { store.answerWhy(if (why == reason) null else reason) }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        AnimatedContent(
            targetState = done,
            transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(200)) },
            modifier = Modifier.fillMaxWidth().height(120.dp),
        ) { finished ->
            if (finished) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WhiteButton("Jeg er ferdig", onStop)
                    Text(
                        "Fortsett til $name",
                        style = MaterialTheme.typography.titleMedium,
                        color = Dusk.TextDim,
                        modifier = Modifier.padding(top = 6.dp).clip(CircleShape).pressable(onContinue).padding(horizontal = 20.dp, vertical = 14.dp),
                    )
                }
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        "Du kan gå ut av Brems. Pausen teller videre.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Dusk.TextFaint,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/**
 * Pusteblomsten: seks gjennomsiktige blader som folder seg ut når man puster inn og samler seg
 * når man puster ut, med en tynn ring rundt som tømmes mens pausen går.
 */
@Composable
private fun Bloom(breath: Float, progress: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val side = size.minDimension
        val ring = side / 2 - 2.dp.toPx()
        val petal = side * (0.17f + 0.07f * breath)
        val spread = side * (0.06f + 0.15f * breath)
        val turn = (breath * 50f - 25f) * (PI / 180).toFloat()
        for (i in 0 until 6) {
            val angle = turn + i * (PI / 3).toFloat()
            val c = Offset(center.x + cos(angle) * spread, center.y + sin(angle) * spread)
            drawCircle(
                Brush.radialGradient(
                    0f to Dusk.Lilac.copy(alpha = 0.30f),
                    1f to Dusk.Violet.copy(alpha = 0.16f),
                    center = c,
                    radius = petal,
                ),
                radius = petal,
                center = c,
            )
        }
        val track = 2.dp.toPx()
        drawCircle(Color.White.copy(alpha = 0.10f), ring, style = Stroke(track))
        if (progress > 0f) {
            drawArc(
                color = Color.White.copy(alpha = 0.9f),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(center.x - ring, center.y - ring),
                size = Size(ring * 2, ring * 2),
                style = Stroke(track, cap = StrokeCap.Round),
            )
        }
    }
}

/** Skumringen bak pausen: dyp indigo øverst, en fiolett glød bak blomsten og en varm glød nederst. */
@Composable
private fun DuskBackground(breath: Float, content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(Dusk.Top, Dusk.Middle, Dusk.Bottom)))
                drawRect(
                    Brush.radialGradient(
                        listOf(Dusk.Violet.copy(alpha = 0.20f + 0.14f * breath), Color.Transparent),
                        center = Offset(size.width / 2, size.height * 0.46f),
                        radius = size.width * (0.75f + 0.1f * breath),
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Dusk.Ember.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width / 2, size.height * 1.08f),
                        radius = size.width * 0.9f,
                    ),
                )
            },
        content = content,
    )
}

@Composable
private fun AppPill(name: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(CircleShape).background(Dusk.Glass).border(1.dp, Dusk.GlassLine, CircleShape).padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(Dusk.Ember))
        Text(name, style = MaterialTheme.typography.labelLarge, color = Dusk.Text, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun GlassIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(38.dp).clip(CircleShape).background(Dusk.Glass).border(1.dp, Dusk.GlassLine, CircleShape).pressable(onClick, pressedScale = 0.88f),
    ) {
        Icon(icon, contentDescription = description, tint = Dusk.Text, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun GlassChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) Dusk.Text else Dusk.Glass)
            .border(1.dp, if (selected) Dusk.Text else Dusk.GlassLine, CircleShape)
            .pressable(onClick, pressedScale = 0.94f, haptic = Haptics::select)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (selected) Dusk.Top else Dusk.Text)
    }
}

@Composable
private fun WhiteButton(text: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(18.dp)).background(Dusk.Text).pressable(onClick, pressedScale = 0.97f),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = Dusk.Top)
    }
}

@Composable
private fun Ending(title: String, body: String, onDone: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.weight(1f))
        Text(title, style = MaterialTheme.typography.displaySmall, color = Dusk.Text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = Dusk.TextDim, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 320.dp))
        Spacer(Modifier.weight(1.3f))
        Box(Modifier.padding(bottom = 16.dp)) { WhiteButton("Ferdig", onDone) }
    }
}

private fun stoppedText(store: BremsStore): String {
    val stopped = store.data.usageToday(store.nowMillis(), TimeZone.currentSystemDefault()).stopped
    val times = if (stopped <= 1) "Første gang i dag." else "$stopped. gang i dag."
    return "Du lot være. $times\nLegg fra deg telefonen og gjør noe annet."
}
