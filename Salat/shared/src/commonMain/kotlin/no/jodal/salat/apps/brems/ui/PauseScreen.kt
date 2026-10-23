package no.jodal.salat.apps.brems.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import no.jodal.salat.ui.pressable

/** Hvor man er etter at pausen er over. */
private enum class PauseStep { Waiting, Stopped, GoBack }

/** Pusten: fire sekunder inn, fire sekunder ut. */
private const val BreathMillis = 4_000L

/**
 * Pausen før en app åpnes: en lysende kule som puster, med nedtellingen inni, og ingenting annet.
 * Den kan ikke hoppes over, men man kan gå ut av Brems: pausen teller videre, og åpner man appen
 * igjen, kommer man tilbake hit. Når tiden er ute, kan man velge å la være eller fortsette.
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
    val breathing = store.data.settings.breathing && !done
    // 0 når man har pustet ut, 1 når man har pustet inn.
    val breath = if (breathing) ((1 - cos(phase * 2 * PI)) / 2).toFloat() else 0.5f
    // Når tiden er ute, trekker kula seg sammen og gjør plass til valget.
    val settle by animateFloatAsState(if (done) 1f else 0f, tween(900))

    Box(
        Modifier
            .fillMaxSize()
            .background(Night.Top)
            .drawBehind {
                // En svak glød nedenfra, rund, så overgangen ikke blir stripete.
                drawRect(
                    Brush.radialGradient(
                        listOf(Night.Bottom, Night.Top),
                        center = Offset(size.width / 2, size.height * 1.1f),
                        radius = size.height * 0.75f,
                    ),
                )
            },
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(44.dp).clip(CircleShape).pressable(onLeave, pressedScale = 0.85f),
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Gå ut av pausen", tint = Night.Faint, modifier = Modifier.size(22.dp))
                }
            }
            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(300)) },
                modifier = Modifier.weight(1f),
            ) { current ->
                when (current) {
                    PauseStep.Waiting -> Waiting(
                        store = store,
                        pause = pause,
                        now = now,
                        seconds = ((remainingMillis + 999) / 1000).toInt(),
                        done = done,
                        breath = breath,
                        settle = settle,
                        cue = if (breathing) (if (phase < 0.5f) "Pust inn" else "Pust ut") else null,
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

/** Fargene på pauseskjermen: natt, med ett lys. */
private object Night {
    val Top = Color(0xFF05050C)
    val Bottom = Color(0xFF110E30)
    val Core = Color(0xFF0E0C2A)
    val Middle = Color(0xFF1E1860)
    val Edge = Color(0xFF6F63FF)
    val Glow = Color(0xFF5A4CF0)
    val Text = Color(0xFFFFFFFF)
    val Dim = Color(0x99FFFFFF)
    val Faint = Color(0x59FFFFFF)
}

@Composable
private fun Waiting(
    store: BremsStore,
    pause: Pause,
    now: Long,
    seconds: Int,
    done: Boolean,
    breath: Float,
    settle: Float,
    cue: String?,
    onContinue: () -> Unit,
    onStop: () -> Unit,
) {
    val settings = store.data.settings
    val name = appName(pause.app)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.weight(1f))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(300.dp)) {
            Orb(breath = breath, scale = 1f - 0.28f * settle)
            Text(
                "$seconds",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 96.sp),
                color = Night.Text,
                modifier = Modifier.alpha(1f - settle),
            )
        }
        Spacer(Modifier.height(24.dp))
        Crossfade(targetState = cue, animationSpec = tween(800)) { text ->
            Text(
                text?.uppercase() ?: "",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 3.sp),
                color = Night.Dim,
                modifier = Modifier.height(16.dp),
            )
        }
        val quote = pause.quote
        if (quote != null && settings.showQuote && !done) {
            Spacer(Modifier.height(28.dp))
            Text(quote, style = MaterialTheme.typography.bodyLarge, color = Night.Dim, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 300.dp))
        }
        Spacer(Modifier.weight(1f))

        AnimatedVisibility(
            visible = done,
            enter = fadeIn(tween(700, delayMillis = 300)) + slideInVertically(tween(700, delayMillis = 300)) { it / 8 },
        ) {
            Choice(store, pause, now, onContinue, onStop)
        }
        if (!done) {
            Text(
                if (pause.kind == PauseKind.Limit) "Over ${formatMinutes(settings.limitMinutes)} i $name. Ta en pause." else "Før $name",
                style = MaterialTheme.typography.bodyMedium,
                color = Night.Faint,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 28.dp),
            )
        }
    }
}

/** Valget når tiden er ute. «Jeg er ferdig» er det store valget, å fortsette er bare tekst. */
@Composable
private fun Choice(store: BremsStore, pause: Pause, now: Long, onContinue: () -> Unit, onStop: () -> Unit) {
    val settings = store.data.settings
    val name = appName(pause.app)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Vil du fortsatt inn på $name?", style = MaterialTheme.typography.headlineSmall, color = Night.Text, textAlign = TextAlign.Center)
        if (settings.showUsage) {
            val today = store.data.usageToday(now, TimeZone.currentSystemDefault())
            Spacer(Modifier.height(8.dp))
            Text(
                "${today.opens}. gang i dag, ${formatDuration(today.millis)} så langt.",
                style = MaterialTheme.typography.bodyMedium,
                color = Night.Dim,
                textAlign = TextAlign.Center,
            )
        }
        if (settings.askWhy && settings.reasons.isNotEmpty()) {
            val why = store.data.pauses.lastOrNull()?.why
            Spacer(Modifier.height(24.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                settings.reasons.forEach { reason ->
                    ReasonChip(reason, selected = why == reason) { store.answerWhy(if (why == reason) null else reason) }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        WhiteButton("Jeg er ferdig", onStop)
        Text(
            "Fortsett til $name",
            style = MaterialTheme.typography.titleMedium,
            color = Night.Dim,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp).clip(CircleShape).pressable(onContinue).padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }
}

/**
 * Kula: mørk inni og lysende i kanten, med en myk glød rundt som vokser når man puster inn.
 * [breath] går fra 0 (pustet ut) til 1 (pustet inn), og [scale] krymper den når pausen er over.
 */
@Composable
private fun Orb(breath: Float, scale: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val max = size.minDimension / 2
        val radius = max * (0.56f + 0.2f * breath) * scale
        // Gløden rundt.
        drawCircle(
            Brush.radialGradient(
                0f to Night.Glow.copy(alpha = 0.34f + 0.16f * breath),
                0.55f to Night.Glow.copy(alpha = 0.10f),
                1f to Color.Transparent,
                center = center,
                radius = radius * 1.75f,
            ),
            radius = radius * 1.75f,
        )
        // Selve kula, mørkest i midten så tallet synes godt, og lysere ut mot kanten.
        drawCircle(
            Brush.radialGradient(
                0f to Night.Core,
                0.62f to Night.Middle,
                1f to Night.Edge.copy(alpha = 0.55f),
                center = center,
                radius = radius,
            ),
            radius = radius,
        )
        // En myk, lysende kant som glir ut i gløden i stedet for en skarp strek.
        drawCircle(
            Brush.radialGradient(
                0f to Color.Transparent,
                0.80f to Color.Transparent,
                0.87f to Night.Edge.copy(alpha = 0.75f),
                0.90f to Color(0xFFB4ACFF).copy(alpha = 0.55f),
                1f to Color.Transparent,
                center = center,
                radius = radius * 1.12f,
            ),
            radius = radius * 1.12f,
        )
        // Et svakt lys oppe til venstre, så den ser rund ut.
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.07f), Color.Transparent),
                center = Offset(center.x - radius * 0.35f, center.y - radius * 0.45f),
                radius = radius * 0.7f,
            ),
            radius = radius * 0.7f,
            center = Offset(center.x - radius * 0.35f, center.y - radius * 0.45f),
        )
    }
}

@Composable
private fun ReasonChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) Night.Text else Color.White.copy(alpha = 0.06f))
            .border(1.dp, if (selected) Night.Text else Color.White.copy(alpha = 0.12f), CircleShape)
            .pressable(onClick, pressedScale = 0.94f, haptic = Haptics::select)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (selected) Night.Top else Night.Text)
    }
}

@Composable
private fun WhiteButton(text: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp)).background(Night.Text).pressable(onClick, pressedScale = 0.97f),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = Night.Top)
    }
}

@Composable
private fun Ending(title: String, body: String, onDone: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.weight(1f))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) { Orb(breath = 0.3f, scale = 0.7f) }
        Spacer(Modifier.height(32.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = Night.Text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = Night.Dim, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 300.dp))
        Spacer(Modifier.weight(1f))
        Box(Modifier.padding(bottom = 24.dp)) { WhiteButton("Ferdig", onDone) }
    }
}

private fun stoppedText(store: BremsStore): String {
    val stopped = store.data.usageToday(store.nowMillis(), TimeZone.currentSystemDefault()).stopped
    val times = if (stopped <= 1) "Første gang i dag." else "$stopped. gang i dag."
    return "Du lot være. $times Legg fra deg telefonen og gjør noe annet."
}
