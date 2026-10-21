package no.jodal.salat.apps.ekko.ui

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import no.jodal.salat.resources.Res
import no.jodal.salat.resources.inter_bold
import no.jodal.salat.resources.inter_medium
import no.jodal.salat.resources.inter_regular
import no.jodal.salat.resources.inter_semibold
import no.jodal.salat.resources.space_grotesk_bold
import no.jodal.salat.resources.space_grotesk_medium
import org.jetbrains.compose.resources.Font
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Ekko sitt uttrykk: mørkt som en musikkspiller, med lime som eneste aksentfarge og Space Grotesk i overskriftene. Hver lyd får sitt
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

/** Space Grotesk til overskrifter, Inter til resten. */
@Composable
private fun ekkoTypography(): Typography {
    val display = FontFamily(
        Font(Res.font.space_grotesk_medium, FontWeight.Medium),
        Font(Res.font.space_grotesk_bold, FontWeight.Bold),
    )
    val text = FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_medium, FontWeight.Medium),
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
        Font(Res.font.inter_bold, FontWeight.Bold),
    )
    fun d(size: Int, spacing: Double, line: Int = (size * 1.15).toInt()) =
        TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp)
    fun t(size: Int, weight: FontWeight, line: Int, spacing: Double = 0.0) =
        TextStyle(fontFamily = text, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp)
    return Typography(
        displaySmall = d(40, -1.6),
        headlineMedium = d(30, -1.0),
        headlineSmall = d(25, -0.7, 30),
        titleLarge = d(21, -0.4, 26),
        titleMedium = t(16, FontWeight.SemiBold, 21, -0.2),
        titleSmall = t(14, FontWeight.SemiBold, 19, -0.1),
        bodyLarge = t(16, FontWeight.Normal, 23, -0.2),
        bodyMedium = t(14, FontWeight.Normal, 20, -0.1),
        bodySmall = t(12, FontWeight.Normal, 16),
        labelLarge = t(14, FontWeight.SemiBold, 18, -0.1),
        labelMedium = t(12, FontWeight.Medium, 16).copy(fontFeatureSettings = "tnum"),
        labelSmall = t(11, FontWeight.SemiBold, 14, 1.0),
    )
}

/** Ekko er alltid mørk, som en musikkspiller. */
@Composable
fun EkkoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = EkkoColors, typography = ekkoTypography()) {
        val selection = TextSelectionColors(handleColor = EkkoPalette.Lime, backgroundColor = EkkoPalette.Lime.copy(alpha = 0.3f))
        CompositionLocalProvider(LocalTextSelectionColors provides selection, content = content)
    }
}
