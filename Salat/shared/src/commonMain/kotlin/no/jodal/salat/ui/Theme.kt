package no.jodal.salat.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight

/** Farger hentet fra salatbollen i logoen. */
object SalatColors {
    val Leaf = Color(0xFF8CD47E)
    val Bowl = Color(0xFFFBEFCF)
    val Cheek = Color(0xFFE8879B)
    val Lemon = Color(0xFFF4D58D)
    val Orange = Color(0xFFF4A259)
    val Lavender = Color(0xFF9B8CE8)
}

private val Light = lightColorScheme(
    primary = Color(0xFF2F7D3F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3F0CB),
    onPrimaryContainer = Color(0xFF0E3A17),
    secondary = Color(0xFFB8485F),
    secondaryContainer = Color(0xFFFFD9DF),
    background = Color(0xFFFFFBF3),
    onBackground = Color(0xFF221C14),
    surface = Color(0xFFFFFBF3),
    onSurface = Color(0xFF221C14),
    surfaceVariant = Color(0xFFF3EBDA),
    onSurfaceVariant = Color(0xFF5E5547),
    surfaceContainer = Color(0xFFF7F0E1),
    outlineVariant = Color(0xFFE3D9C5),
    error = Color(0xFFC62828),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF9EDC91),
    onPrimary = Color(0xFF0E3A17),
    primaryContainer = Color(0xFF1F4D29),
    onPrimaryContainer = Color(0xFFD3F0CB),
    secondary = Color(0xFFF2A7B6),
    secondaryContainer = Color(0xFF5E2433),
    background = Color(0xFF17140F),
    onBackground = Color(0xFFEFE6D6),
    surface = Color(0xFF17140F),
    onSurface = Color(0xFFEFE6D6),
    surfaceVariant = Color(0xFF2A251D),
    onSurfaceVariant = Color(0xFFCFC4B1),
    surfaceContainer = Color(0xFF221E17),
    outlineVariant = Color(0xFF3D362B),
    error = Color(0xFFFF8A80),
)

@Composable
fun SalatTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val base = Typography()
    MaterialTheme(
        colorScheme = if (darkTheme) Dark else Light,
        typography = base.copy(
            displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold),
            headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
            titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
            titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        ),
        content = content,
    )
}

/** En dempet versjon av [color] som passer som bakgrunn i både lys og mørk modus. */
fun ColorScheme.tint(color: Color, darkTheme: Boolean): Color =
    lerp(surface, color, if (darkTheme) 0.22f else 0.30f)
