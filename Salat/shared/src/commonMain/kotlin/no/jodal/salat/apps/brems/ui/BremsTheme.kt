package no.jodal.salat.apps.brems.ui

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
import androidx.compose.ui.unit.sp
import no.jodal.salat.resources.Res
import no.jodal.salat.resources.inter_bold
import no.jodal.salat.resources.inter_medium
import no.jodal.salat.resources.inter_regular
import no.jodal.salat.resources.inter_semibold
import no.jodal.salat.resources.space_grotesk_bold
import no.jodal.salat.resources.space_grotesk_medium
import org.jetbrains.compose.resources.Font

/**
 * Brems sitt uttrykk: nesten svart, med iskald blå som eneste signalfarge, store tall i Space Grotesk og
 * mye luft. Rolig og fokusert, som en klokke.
 */
object BremsPalette {
    val Ink = Color(0xFF050608)
    val Surface = Color(0xFF0E1115)
    val SurfaceHigh = Color(0xFF181C22)
    val Line = Color(0xFF1B2027)
    val Text = Color(0xFFF2F5F8)
    val TextDim = Color(0xFF7D8794)
    val TextFaint = Color(0xFF454D58)
    val Ice = Color(0xFF8FDBFF)
    val IceDeep = Color(0xFF2C5F9E)
    val OnIce = Color(0xFF05111B)
    val Warn = Color(0xFFFFB86B)
}

private val BremsColors = darkColorScheme(
    primary = BremsPalette.Ice,
    onPrimary = BremsPalette.OnIce,
    secondary = BremsPalette.Ice,
    background = BremsPalette.Ink,
    onBackground = BremsPalette.Text,
    surface = BremsPalette.Ink,
    onSurface = BremsPalette.Text,
    surfaceContainer = BremsPalette.Surface,
    surfaceContainerLow = BremsPalette.Surface,
    surfaceContainerHigh = BremsPalette.SurfaceHigh,
    surfaceVariant = BremsPalette.Surface,
    onSurfaceVariant = BremsPalette.TextDim,
    outlineVariant = BremsPalette.Line,
    error = BremsPalette.Warn,
)

/** Space Grotesk til tall og overskrifter, Inter til resten. */
@Composable
private fun bremsTypography(): Typography {
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
    fun d(size: Int, spacing: Double, weight: FontWeight = FontWeight.Medium, line: Int = (size * 1.1).toInt()) =
        TextStyle(fontFamily = display, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp)
    fun t(size: Int, weight: FontWeight, line: Int, spacing: Double = 0.0) =
        TextStyle(fontFamily = text, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp)
    return Typography(
        displayLarge = d(88, -4.0),
        displayMedium = d(56, -2.4),
        displaySmall = d(40, -1.6),
        headlineMedium = d(30, -1.0, FontWeight.Bold),
        headlineSmall = d(24, -0.6, FontWeight.Bold, 30),
        titleLarge = d(20, -0.3, FontWeight.Bold, 26),
        titleMedium = t(16, FontWeight.SemiBold, 21, -0.2),
        titleSmall = t(14, FontWeight.SemiBold, 19, -0.1),
        bodyLarge = t(16, FontWeight.Normal, 24, -0.2),
        bodyMedium = t(14, FontWeight.Normal, 20, -0.1),
        bodySmall = t(12, FontWeight.Normal, 16),
        labelLarge = t(14, FontWeight.SemiBold, 18, -0.1),
        labelMedium = t(12, FontWeight.Medium, 16).copy(fontFeatureSettings = "tnum"),
        labelSmall = t(11, FontWeight.SemiBold, 14, 1.2),
    )
}

/** Brems er alltid mørk. */
@Composable
fun BremsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BremsColors, typography = bremsTypography()) {
        val selection = TextSelectionColors(handleColor = BremsPalette.Ice, backgroundColor = BremsPalette.Ice.copy(alpha = 0.3f))
        CompositionLocalProvider(LocalTextSelectionColors provides selection, content = content)
    }
}
