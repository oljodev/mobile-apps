package no.jodal.salat.apps.kompis.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import no.jodal.salat.ui.LocalDarkTheme

/**
 * Kompis sitt uttrykk: stramt og nesten fargeløst, med et nordlys i blått, fiolett og rosa som eneste
 * farge. Nordlyset brukes bare der stemmen er.
 */
object KompisPalette {
    val Cyan = Color(0xFF3DD6F5)
    val Blue = Color(0xFF4F7CFF)
    val Violet = Color(0xFF9B5CFF)
    val Pink = Color(0xFFFF5CA8)
    val EndCall = Color(0xFFFF453A)

    /** Samtaleskjermen er alltid svart, som en telefonsamtale om natten. */
    val Night = Color(0xFF000000)
    val NightSurface = Color(0xFF1C1C1F)
    val NightSecondary = Color(0xFF8E8E93)

    val Aurora = listOf(Cyan, Blue, Violet, Pink)
}

private val KompisLight = lightColorScheme(
    primary = Color(0xFF0A0A0A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF2F2F4),
    onPrimaryContainer = Color(0xFF0A0A0A),
    secondary = KompisPalette.Blue,
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF0A0A0A),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF0A0A0A),
    surfaceContainer = Color.White,
    surfaceVariant = Color(0xFFF0F0F2),
    onSurfaceVariant = Color(0xFF6E6E73),
    outlineVariant = Color(0xFFE6E6EA),
    error = Color(0xFFE5342B),
)

private val KompisDark = darkColorScheme(
    primary = Color(0xFFF5F5F7),
    onPrimary = Color(0xFF0A0A0A),
    primaryContainer = Color(0xFF1C1C1F),
    onPrimaryContainer = Color(0xFFF5F5F7),
    secondary = Color(0xFF8C9DFF),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF5F5F7),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFF5F5F7),
    surfaceContainer = Color(0xFF111113),
    surfaceVariant = Color(0xFF1C1C1F),
    onSurfaceVariant = Color(0xFF8E8E93),
    outlineVariant = Color(0xFF232326),
    error = Color(0xFFFF6961),
)

@Composable
fun KompisTheme(dark: Boolean = LocalDarkTheme.current, content: @Composable () -> Unit) {
    val base = MaterialTheme.typography
    MaterialTheme(
        colorScheme = if (dark) KompisDark else KompisLight,
        typography = Typography(
            displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp),
            headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
            titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
            titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            bodyLarge = base.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
            bodyMedium = base.bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp),
            bodySmall = base.bodySmall,
            labelLarge = base.labelLarge,
            labelMedium = base.labelMedium,
            labelSmall = base.labelSmall,
        ),
        content = content,
    )
}
