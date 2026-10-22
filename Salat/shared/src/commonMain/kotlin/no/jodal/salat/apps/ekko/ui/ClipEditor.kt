package no.jodal.salat.apps.ekko.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.floor
import no.jodal.salat.Haptics
import no.jodal.salat.apps.ekko.EkkoPlayer
import no.jodal.salat.apps.ekko.formatLength
import no.jodal.salat.apps.ekko.formatPrecise

/** Den korteste lyden man kan klippe, i sekunder. */
private const val MinLength = 0.3

/** Hvor mye knappene ved start og slutt flytter, i sekunder. */
private const val Nudge = 0.1

/** Klipp lyden, hør på utsnittet og gi den navn. Brukes både for nye lyder og for å redigere. */
@Composable
fun ClipEditor(
    heading: String,
    clipId: String,
    initialTitle: String,
    initialArtist: String,
    sourceSeconds: Double,
    waveform: List<Float>,
    initialStart: Double,
    initialEnd: Double,
    player: EkkoPlayer,
    onCancel: () -> Unit,
    onSave: (title: String, artist: String, start: Double, end: Double) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var artist by rememberSaveable { mutableStateOf(initialArtist) }
    var start by remember { mutableStateOf(initialStart) }
    var end by remember { mutableStateOf(initialEnd) }
    val preview = player.preview?.takeIf { it.clipId == clipId }

    DisposableEffect(Unit) { onDispose { player.stopPreview() } }

    fun setRange(newStart: Double, newEnd: Double) {
        start = newStart
        end = newEnd
        // Utsnittet man hører på stemmer ikke lenger.
        player.stopPreview()
    }

    Column(Modifier.fillMaxSize().background(EkkoPalette.Ink).imePadding()) {
        EditorTopBar(
            title = heading,
            onCancel = onCancel,
            onSave = { onSave(title, artist, start, end) },
            canSave = end - start >= MinLength,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(top = 8.dp, bottom = 32.dp),
        ) {
            SectionLabel("Klipp")
            Trimmer(
                waveform = waveform,
                duration = sourceSeconds,
                start = start,
                end = end,
                playhead = preview?.positionSeconds,
                onChange = ::setRange,
            )
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                TimeNudger(
                    label = "Start",
                    seconds = start,
                    onMinus = { setRange((start - Nudge).coerceAtLeast(0.0), end) },
                    onPlus = { setRange((start + Nudge).coerceAtMost(end - MinLength), end) },
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    SectionLabel("Lengde")
                    Text(formatLength(end - start), style = MaterialTheme.typography.titleMedium, color = EkkoPalette.Lime, modifier = Modifier.padding(vertical = 4.dp))
                }
                TimeNudger(
                    label = "Slutt",
                    seconds = end,
                    alignEnd = true,
                    onMinus = { setRange(start, (end - Nudge).coerceAtLeast(start + MinLength)) },
                    onPlus = { setRange(start, (end + Nudge).coerceAtMost(sourceSeconds)) },
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
                    .clip(CircleShape)
                    .background(EkkoPalette.Surface)
                    .pressable({ if (preview != null) player.stopPreview() else player.startPreview(clipId, start, end) })
                    .padding(start = 14.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
            ) {
                Icon(
                    if (preview != null) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = EkkoPalette.Lime,
                    modifier = Modifier.size(24.dp),
                )
                Text(if (preview != null) "Stopp" else "Hør utsnittet", style = MaterialTheme.typography.titleSmall, color = EkkoPalette.Text)
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("Navn")
            EkkoTextField(title, { title = it }, placeholder = "Hva heter lyden?")
            Spacer(Modifier.height(4.dp))
            SectionLabel("Hvem sier det?")
            EkkoTextField(artist, { artist = it }, placeholder = "Valgfritt")
        }
    }
}

/**
 * Lydbølgen for hele videoen med en ramme rundt utsnittet. Dra i kantene for å flytte start og
 * slutt, eller midt i for å flytte hele utsnittet.
 */
@Composable
private fun Trimmer(
    waveform: List<Float>,
    duration: Double,
    start: Double,
    end: Double,
    playhead: Double?,
    onChange: (Double, Double) -> Unit,
) {
    val currentStart by rememberUpdatedState(start)
    val currentEnd by rememberUpdatedState(end)
    val currentOnChange by rememberUpdatedState(onChange)

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(EkkoPalette.Surface),
    ) {
        val density = LocalDensity.current
        val handle = with(density) { 16.dp.toPx() }
        val width = constraints.maxWidth.toFloat()
        val track = width - 2 * handle
        val bars = (maxWidth.value / 4.5f).toInt().coerceAtLeast(20)
        val sampled = remember(waveform, bars) { resample(waveform, bars) }
        fun xOf(seconds: Double): Float = handle + (seconds / duration * track).toFloat()

        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(duration, width) {
                    // 1 = start, 2 = slutt, 3 = hele utsnittet.
                    var mode = 0
                    val reach = 28.dp.toPx()
                    detectDragGestures(
                        onDragStart = { offset ->
                            Haptics.select()
                            val startX = xOf(currentStart)
                            val endX = xOf(currentEnd)
                            mode = when {
                                abs(offset.x - startX) < reach && abs(offset.x - startX) <= abs(offset.x - endX) -> 1
                                abs(offset.x - endX) < reach -> 2
                                offset.x in startX..endX -> 3
                                else -> 0
                            }
                        },
                    ) { change, amount ->
                        change.consume()
                        val delta = amount.x / track * duration
                        val s = currentStart
                        val e = currentEnd
                        val (newStart, newEnd) = when (mode) {
                            1 -> (s + delta).coerceIn(0.0, e - MinLength) to e
                            2 -> s to (e + delta).coerceIn(s + MinLength, duration)
                            3 -> delta.coerceIn(-s, duration - e).let { s + it to e + it }
                            else -> return@detectDragGestures
                        }
                        // Et lite klikk for hvert halve sekund, og et fastere når man treffer kanten.
                        val moved = if (mode == 2) e to newEnd else s to newStart
                        val hitEdge = (newStart == 0.0 && s > 0.0) || (newEnd == duration && e < duration)
                        if (hitEdge) Haptics.firm() else if (floor(moved.first * 2) != floor(moved.second * 2)) Haptics.select()
                        currentOnChange(newStart, newEnd)
                    }
                },
        ) {
            val slot = track / bars
            val barWidth = slot * 0.6f
            sampled.forEachIndexed { i, level ->
                val t = (i + 0.5) / bars * duration
                val inside = t in start..end
                val h = size.height * 0.62f * level.coerceIn(0.08f, 1f)
                drawRoundRect(
                    color = if (inside) EkkoPalette.Text else EkkoPalette.TextDim.copy(alpha = 0.35f),
                    topLeft = Offset(handle + i * slot + (slot - barWidth) / 2, (size.height - h) / 2),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(barWidth / 2),
                )
            }

            val startX = xOf(start)
            val endX = xOf(end)
            val corner = CornerRadius(12.dp.toPx())
            val stroke = 3.dp.toPx()
            drawRoundRect(
                color = EkkoPalette.Lime,
                topLeft = Offset(startX - handle + stroke / 2, stroke / 2),
                size = Size(endX - startX + 2 * handle - stroke, size.height - stroke),
                cornerRadius = corner,
                style = Stroke(stroke),
            )
            for (x in listOf(startX - handle, endX)) {
                drawRoundRect(EkkoPalette.Lime, Offset(x, 0f), Size(handle, size.height), corner)
                drawLine(
                    color = EkkoPalette.OnLime.copy(alpha = 0.7f),
                    start = Offset(x + handle / 2, size.height / 2 - 11.dp.toPx()),
                    end = Offset(x + handle / 2, size.height / 2 + 11.dp.toPx()),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            if (playhead != null) {
                val x = xOf(playhead)
                drawLine(Color.White, Offset(x, 8.dp.toPx()), Offset(x, size.height - 8.dp.toPx()), 2.5.dp.toPx(), StrokeCap.Round)
            }
        }
    }
}

/** «Start 0:03.2» med knapper for å flytte en tidel av gangen. */
@Composable
private fun TimeNudger(label: String, seconds: Double, onMinus: () -> Unit, onPlus: () -> Unit, alignEnd: Boolean = false) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionLabel(label)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RoundIconButton(Icons.Rounded.Remove, "$label tidligere", onMinus, size = 30.dp, iconSize = 16.dp, haptic = Haptics::select)
            Text(formatPrecise(seconds), style = MaterialTheme.typography.titleMedium, color = EkkoPalette.Text)
            RoundIconButton(Icons.Rounded.Add, "$label senere", onPlus, size = 30.dp, iconSize = 16.dp, haptic = Haptics::select)
        }
    }
}
