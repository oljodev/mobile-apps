package no.jodal.salat.apps.kompis.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/** Hva stemme-visualiseringen viser. */
enum class VoiceMode { Idle, Listening, Thinking, Speaking }

private fun VoiceMode.colors(): List<Color> = when (this) {
    VoiceMode.Idle -> listOf(KompisPalette.Blue, KompisPalette.Violet, KompisPalette.Blue)
    VoiceMode.Listening -> listOf(KompisPalette.Cyan, KompisPalette.Blue, KompisPalette.Cyan)
    VoiceMode.Thinking -> listOf(KompisPalette.Blue, KompisPalette.Violet, KompisPalette.Pink)
    VoiceMode.Speaking -> listOf(KompisPalette.Violet, KompisPalette.Pink, KompisPalette.Blue)
}

/** Hvor mye nordlyset lyser uten lyd. */
private fun VoiceMode.baseEnergy(): Float = when (this) {
    VoiceMode.Idle -> 0.15f
    VoiceMode.Listening -> 0.35f
    VoiceMode.Thinking -> 0.45f
    VoiceMode.Speaking -> 0.5f
}

/**
 * Et nordlys som glør nedenfra og beveger seg sakte sidelengs. Det stiger og lyser sterkere med
 * [level] (lydnivået fra 0 til 1), og skifter farge med hvem som snakker.
 */
@Composable
fun Aurora(mode: VoiceMode, level: Float, modifier: Modifier = Modifier) {
    val targets = mode.colors()
    val c0 by animateColorAsState(targets[0], tween(900))
    val c1 by animateColorAsState(targets[1], tween(900))
    val c2 by animateColorAsState(targets[2], tween(900))
    val energy by animateFloatAsState(
        (mode.baseEnergy() + level.coerceIn(0f, 1f) * 0.7f).coerceAtMost(1f),
        spring(stiffness = 120f),
    )
    val transition = rememberInfiniteTransition()
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(if (mode == VoiceMode.Thinking) 6000 else 14000, easing = LinearEasing)),
    )

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val colors = listOf(c0, c1, c2, c1)
        // Fire myke lysflekker som flyter langs bunnen og overlapper hverandre.
        for (i in colors.indices) {
            val phase = i * 1.7f
            val x = w * (0.1f + 0.27f * i) + sin(time + phase) * w * 0.12f
            // Lyset ligger under kanten og stiger litt opp når noen snakker.
            val y = h * (1.22f - 0.3f * energy) + sin(time * 2 + phase) * h * 0.03f
            val radius = w * (0.5f + 0.16f * energy)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(colors[i].copy(alpha = 0.5f * (0.35f + 0.65f * energy)), colors[i].copy(alpha = 0f)),
                    center = Offset(x, y),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(x, y),
            )
        }
    }
}

/** Fem små streker som beveger seg med lyden, som et lite lydbølge-ikon. */
@Composable
fun VoiceBars(level: Float, color: Color, modifier: Modifier = Modifier, barWidth: Dp = 3.dp, animated: Boolean = true) {
    val transition = rememberInfiniteTransition()
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
    )
    val smooth by animateFloatAsState(level.coerceIn(0f, 1f), spring(stiffness = 300f))
    val shape = listOf(0.35f, 0.65f, 1f, 0.65f, 0.35f)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(barWidth * 0.8f), verticalAlignment = Alignment.CenterVertically) {
        shape.forEachIndexed { i, base ->
            val wave = if (animated) (0.75f + 0.25f * sin(time + i * 1.2f)) else 1f
            val height = (base * wave * (0.45f + 0.55f * maxOf(smooth, if (animated) 0.25f else 1f))).coerceIn(0.15f, 1f)
            Row(
                Modifier
                    .width(barWidth)
                    .fillMaxHeight(height)
                    .clip(CircleShape)
                    .background(color),
            ) {}
        }
    }
}
