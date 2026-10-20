package no.jodal.salat.apps.ekko.ui

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Ekko sitt uttrykk: mørkt som en musikkspiller, med lime som eneste aksentfarge. Hver lyd får sitt
 * eget omslag i to farger, så listene blir fargerike uten bilder.
 */
object EkkoPalette {
    val Ink = Color(0xFF0B0B0E)
    val Surface = Color(0xFF16161B)
    val SurfaceHigh = Color(0xFF212128)
    val Line = Color(0xFF2A2A32)
    val Text = Color(0xFFF4F4F6)
    val TextDim = Color(0xFF8E8E9A)
    val Lime = Color(0xFFC8F560)
    val OnLime = Color(0xFF0B0B0E)
    val Danger = Color(0xFFFF6B6B)

    private val Covers = listOf(
        Color(0xFFC8F560) to Color(0xFF2FB58A),
        Color(0xFFFF8A5B) to Color(0xFFE5487A),
        Color(0xFF8C7BFF) to Color(0xFF4A5BFF),
        Color(0xFFFFD25E) to Color(0xFFFF8A3D),
        Color(0xFF5EE1F0) to Color(0xFF3A7BFF),
        Color(0xFFFF7AC6) to Color(0xFF9B5CFF),
        Color(0xFF7CF0B5) to Color(0xFF2FA8C9),
    )

    /** De to fargene i omslaget til lyden eller spillelisten med [id]. Samme id gir alltid samme farger. */
    fun cover(id: String): Pair<Color, Color> = Covers[abs(id.hashCode()) % Covers.size]
}

private val EkkoColors = darkColorScheme(
    primary = EkkoPalette.Lime,
    onPrimary = EkkoPalette.OnLime,
    secondary = EkkoPalette.Lime,
    background = EkkoPalette.Ink,
    onBackground = EkkoPalette.Text,
    surface = EkkoPalette.Ink,
    onSurface = EkkoPalette.Text,
    surfaceContainer = EkkoPalette.Surface,
    surfaceContainerLow = EkkoPalette.Surface,
    surfaceContainerHigh = EkkoPalette.SurfaceHigh,
    surfaceVariant = EkkoPalette.Surface,
    onSurfaceVariant = EkkoPalette.TextDim,
    outlineVariant = EkkoPalette.Line,
    error = EkkoPalette.Danger,
)

/** Ekko er alltid mørk, som en musikkspiller. */
@Composable
fun EkkoTheme(content: @Composable () -> Unit) {
    val base = MaterialTheme.typography
    MaterialTheme(
        colorScheme = EkkoColors,
        typography = Typography(
            displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-1.2).sp),
            headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp),
            headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
            titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
            titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
            titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            bodyLarge = base.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
            bodyMedium = base.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
            bodySmall = base.bodySmall,
            labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            labelMedium = base.labelMedium.copy(fontWeight = FontWeight.Medium),
            labelSmall = base.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp),
        ),
    ) {
        val selection = TextSelectionColors(handleColor = EkkoPalette.Lime, backgroundColor = EkkoPalette.Lime.copy(alpha = 0.3f))
        CompositionLocalProvider(LocalTextSelectionColors provides selection, content = content)
    }
}
