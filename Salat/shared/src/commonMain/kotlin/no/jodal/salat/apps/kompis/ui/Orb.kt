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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Hva kula viser. */
enum class OrbMode { Idle, Listening, Thinking, Speaking }

private val Deep = Color(0xFF120C34)

/** Fire farger per tilstand: de tre første blir flytende nordlys inni kula, den siste er en liten aksent. */
private fun OrbMode.colors(): List<Color> = when (this) {
    OrbMode.Idle -> listOf(KompisPalette.Violet, KompisPalette.Indigo, KompisPalette.Orchid, KompisPalette.Aqua)
    OrbMode.Listening -> listOf(KompisPalette.Aqua, KompisPalette.Sky, KompisPalette.Indigo, KompisPalette.Violet)
    OrbMode.Thinking -> listOf(KompisPalette.Indigo, KompisPalette.Violet, KompisPalette.Sky, KompisPalette.Orchid)
    OrbMode.Speaking -> listOf(KompisPalette.Violet, KompisPalette.Orchid, KompisPalette.Aqua, KompisPalette.Indigo)
}

/**
 * En levende glasskule med flytende nordlys inni. Den puster rolig av seg selv, glitrer, og vokser og lyser
 * sterkere med [level] (lydnivået fra 0 til 1) mens noen snakker. Ringer sprer seg utover når Kompis lytter
 * eller snakker.
 */
@Composable
fun Orb(mode: OrbMode, level: Float, modifier: Modifier = Modifier) {
    val targets = mode.colors()
    val c0 by animateColorAsState(targets[0], tween(700))
    val c1 by animateColorAsState(targets[1], tween(700))
    val c2 by animateColorAsState(targets[2], tween(700))
    val c3 by animateColorAsState(targets[3], tween(700))
    val smoothLevel by animateFloatAsState(level.coerceIn(0f, 1f), spring(stiffness = 300f))
    val active by animateFloatAsState(if (mode == OrbMode.Listening || mode == OrbMode.Speaking) 1f else 0f, tween(500))

    val transition = rememberInfiniteTransition()
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(if (mode == OrbMode.Thinking) 5000 else 16000, easing = LinearEasing)),
    )
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(if (mode == OrbMode.Thinking) 2400 else 12000, easing = LinearEasing)),
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
    )
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse),
    )

    // Blendemodusene trenger et eget lag, ellers blandes fargene med det som ligger bak kula.
    Canvas(modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 * 0.56f * (1f + 0.035f * breath + 0.18f * smoothLevel)
        val glow = 0.3f + 0.35f * smoothLevel

        drawHalo(center, radius, c0, c2, glow)
        drawRipples(center, radius, c0, c3, pulse, active, smoothLevel)

        val sphere = Path().apply { addOval(Rect(center, radius)) }
        clipPath(sphere) {
            drawBackground(center, radius)
            drawAurora(center, radius, listOf(c0, c1, c2, c3), drift, smoothLevel)
            drawDepth(center, radius)
        }
        drawGlassRim(center, radius, c3, spin)
        drawHighlights(center, radius)
        drawSparkles(center, radius, c3, drift, pulse, smoothLevel)
    }
}

/** Mykt lys rundt kula. */
private fun DrawScope.drawHalo(center: Offset, radius: Float, a: Color, b: Color, glow: Float) {
    val reach = radius * 2.1f
    drawCircle(
        brush = Brush.radialGradient(
            0f to a.copy(alpha = glow),
            0.45f to b.copy(alpha = glow * 0.35f),
            1f to Color.Transparent,
            center = center,
            radius = reach,
        ),
        radius = reach,
        center = center,
    )
}

/** Tynne ringer som sprer seg ut fra kula mens noen snakker. */
private fun DrawScope.drawRipples(center: Offset, radius: Float, a: Color, b: Color, pulse: Float, active: Float, level: Float) {
    if (active < 0.01f) return
    for (i in 0 until 3) {
        val phase = (pulse + i / 3f) % 1f
        val alpha = (1f - phase) * (1f - phase) * active * (0.3f + 0.4f * level)
        drawCircle(
            brush = Brush.sweepGradient(listOf(a.copy(alpha = alpha), b.copy(alpha = alpha * 0.4f), a.copy(alpha = alpha)), center),
            radius = radius * (1.02f + 0.75f * phase),
            center = center,
            style = Stroke(width = radius * 0.02f),
        )
    }
}

private fun DrawScope.drawBackground(center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFF2B2070), Deep, Color(0xFF07051A)),
            center = center + Offset(-radius * 0.25f, -radius * 0.3f),
            radius = radius * 1.5f,
        ),
        radius = radius,
        center = center,
    )
}

/** Flytende fargeflekker som blandes lyst oppå hverandre, som nordlys. */
private fun DrawScope.drawAurora(center: Offset, radius: Float, colors: List<Color>, drift: Float, level: Float) {
    val reach = radius * (0.5f + 0.25f * level)
    val blobRadius = radius * (0.82f + 0.2f * level)
    for (i in 0 until 5) {
        val color = colors[i % colors.size]
        val phase = i * 1.37f
        val offset = Offset(
            cos(drift * (1 + i % 2) + phase) * reach,
            sin(drift * (2 - i % 2) * 0.8f + phase * 1.6f) * reach,
        )
        val at = center + offset
        drawCircle(
            brush = Brush.radialGradient(
                listOf(color.copy(alpha = 0.78f), color.copy(alpha = 0.28f), Color.Transparent),
                center = at,
                radius = blobRadius,
            ),
            radius = blobRadius,
            center = at,
            blendMode = BlendMode.Plus,
        )
    }
}

/** Skygge mot kanten og et svakt lys nede til høyre, så kula får volum. */
private fun DrawScope.drawDepth(center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            0.55f to Color.Transparent,
            1f to Deep.copy(alpha = 0.55f),
            center = center + Offset(-radius * 0.12f, -radius * 0.15f),
            radius = radius * 1.05f,
        ),
        radius = radius,
        center = center,
    )
}

/** Kanten av glasset: en tynn lysring som skinner mest der lyset treffer, og som sakte går rundt. */
private fun DrawScope.drawGlassRim(center: Offset, radius: Float, accent: Color, spin: Float) {
    val width = radius * 0.035f
    rotate(spin, center) {
        drawCircle(
            brush = Brush.sweepGradient(
                0f to Color.White.copy(alpha = 0.85f),
                0.25f to accent.copy(alpha = 0.25f),
                0.5f to Color.White.copy(alpha = 0.1f),
                0.75f to accent.copy(alpha = 0.45f),
                1f to Color.White.copy(alpha = 0.85f),
                center = center,
            ),
            radius = radius - width / 2,
            center = center,
            style = Stroke(width = width),
        )
    }
    drawCircle(
        color = Color.White.copy(alpha = 0.12f),
        radius = radius - width * 2.2f,
        center = center,
        style = Stroke(width = width * 0.6f),
    )
}

/** Lysrefleksen øverst til venstre som får kula til å se ut som glass. */
private fun DrawScope.drawHighlights(center: Offset, radius: Float) {
    val at = center + Offset(-radius * 0.38f, -radius * 0.5f)
    rotate(-35f, at) {
        val w = radius * 0.62f
        val h = radius * 0.32f
        drawOval(
            brush = Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.75f), Color.White.copy(alpha = 0f)),
                center = at,
                radius = w / 2,
            ),
            topLeft = at - Offset(w / 2, h / 2),
            size = Size(w, h),
        )
    }
    drawCircle(Color.White.copy(alpha = 0.9f), radius = radius * 0.035f, center = center + Offset(-radius * 0.58f, -radius * 0.28f))
}

/** Små stjerner som sirkler rundt kula og tindrer. */
private fun DrawScope.drawSparkles(center: Offset, radius: Float, accent: Color, drift: Float, pulse: Float, level: Float) {
    for (i in 0 until 5) {
        val angle = drift * (if (i % 2 == 0) 1f else -0.7f) + i * 2 * PI.toFloat() / 5f
        val distance = radius * (1.28f + 0.12f * ((i * 37) % 5) / 4f + 0.08f * level)
        val at = center + Offset(cos(angle) * distance, sin(angle) * distance * 0.9f)
        val twinkle = abs(sin((pulse * 2f + i * 0.37f) * PI.toFloat()))
        val size = radius * (0.05f + 0.07f * twinkle) * (0.8f + 0.5f * level)
        drawStar(at, size, (if (i % 2 == 0) Color.White else accent).copy(alpha = 0.35f + 0.65f * twinkle))
    }
}

/** En firspiss stjerne. */
private fun DrawScope.drawStar(at: Offset, size: Float, color: Color) {
    val waist = size * 0.22f
    val path = Path().apply {
        moveTo(at.x, at.y - size)
        quadraticTo(at.x + waist, at.y - waist, at.x + size, at.y)
        quadraticTo(at.x + waist, at.y + waist, at.x, at.y + size)
        quadraticTo(at.x - waist, at.y + waist, at.x - size, at.y)
        quadraticTo(at.x - waist, at.y - waist, at.x, at.y - size)
        close()
    }
    drawCircle(color.copy(alpha = color.alpha * 0.25f), radius = size * 1.6f, center = at)
    drawPath(path, color)
}
