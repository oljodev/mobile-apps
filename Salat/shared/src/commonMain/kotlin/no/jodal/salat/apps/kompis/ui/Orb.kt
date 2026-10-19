package no.jodal.salat.apps.kompis.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Hva kula viser. */
enum class OrbMode { Idle, Listening, Thinking, Speaking }

/**
 * En levende kule som viser hvem som snakker. Den puster rolig av seg selv, og vokser med [level]
 * (lydnivået fra 0 til 1) mens noen snakker.
 */
@Composable
fun Orb(mode: OrbMode, level: Float, modifier: Modifier = Modifier) {
    val (primaryTarget, secondaryTarget) = when (mode) {
        OrbMode.Idle -> KompisPalette.Violet to KompisPalette.Indigo
        OrbMode.Listening -> KompisPalette.Aqua to KompisPalette.Sky
        OrbMode.Thinking -> KompisPalette.Indigo to KompisPalette.Violet
        OrbMode.Speaking -> KompisPalette.Violet to KompisPalette.Orchid
    }
    val primary by animateColorAsState(primaryTarget, tween(600))
    val secondary by animateColorAsState(secondaryTarget, tween(600))
    val smoothLevel by animateFloatAsState(level.coerceIn(0f, 1f), spring(stiffness = 300f))

    val transition = rememberInfiniteTransition()
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(if (mode == OrbMode.Thinking) 3000 else 9000, easing = LinearEasing)),
    )
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
    )

    Canvas(modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val base = size.minDimension / 2 * 0.56f
        val radius = base * (1f + 0.04f * breath + 0.22f * smoothLevel)

        // Glød rundt kula.
        drawCircle(
            brush = Brush.radialGradient(
                listOf(primary.copy(alpha = 0.32f + 0.25f * smoothLevel), primary.copy(alpha = 0.08f), Color.Transparent),
                center = center,
                radius = radius * 1.9f,
            ),
            radius = radius * 1.9f,
            center = center,
        )

        // Tre myke flekker som sirkler inni kula, så den ser levende ut.
        val colors = listOf(primary, secondary, KompisPalette.Orchid.copy(alpha = 0.8f))
        for (i in colors.indices) {
            val a = angle + i * (2 * PI / 3).toFloat()
            val wobble = radius * (0.16f + 0.14f * smoothLevel)
            val offset = Offset(cos(a) * wobble, sin(a * 1.3f) * wobble)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(colors[i].copy(alpha = 0.85f), colors[i].copy(alpha = 0f)),
                    center = center + offset,
                    radius = radius * 0.95f,
                ),
                radius = radius * 0.95f,
                center = center + offset,
            )
        }

        // Selve kula, med lys oppe til venstre.
        drawCircle(
            brush = Brush.linearGradient(
                listOf(primary.copy(alpha = 0.95f), secondary.copy(alpha = 0.9f)),
                start = center + Offset(-radius, -radius),
                end = center + Offset(radius, radius),
            ),
            radius = radius * 0.86f,
            center = center,
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0f)),
                center = center + Offset(-radius * 0.32f, -radius * 0.38f),
                radius = radius * 0.6f,
            ),
            radius = radius * 0.6f,
            center = center + Offset(-radius * 0.32f, -radius * 0.38f),
        )
    }
}
