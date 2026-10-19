package no.jodal.salat.apps.kompis.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import no.jodal.salat.ui.LocalDarkTheme

/** Kompis sitt uttrykk: rolig fiolett med turkis, og en mørk samtaleskjerm som en telefonsamtale. */
object KompisPalette {
    val Violet = Color(0xFFA78BFA)
    val Orchid = Color(0xFFF0ABFC)
    val Aqua = Color(0xFF5EEAD4)
    val Sky = Color(0xFF38BDF8)
    val Indigo = Color(0xFF818CF8)
    val EndCall = Color(0xFFEF4444)

    /** Bakgrunnen på samtaleskjermen, alltid mørk. */
    val Night = Brush.verticalGradient(listOf(Color(0xFF07060F), Color(0xFF120F26), Color(0xFF0B0A16)))

    /** Velkomstkortet på oversikten. */
    fun welcome(darkTheme: Boolean): Brush = Brush.linearGradient(
        if (darkTheme) {
            listOf(Color(0xFF2A2160), Color(0xFF3B2A78), Color(0xFF155E63))
        } else {
            listOf(Color(0xFF6D5BD0), Color(0xFF8B6CF0), Color(0xFF2BB5A8))
        },
    )
}

private val KompisLight = lightColorScheme(
    primary = Color(0xFF6D5BD0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAE5FF),
    onPrimaryContainer = Color(0xFF231A5C),
    secondary = Color(0xFF14958A),
    background = Color(0xFFF7F6FB),
    onBackground = Color(0xFF1B1A22),
    surface = Color(0xFFF7F6FB),
    onSurface = Color(0xFF1B1A22),
    surfaceContainer = Color.White,
    surfaceVariant = Color(0xFFEEECF5),
    onSurfaceVariant = Color(0xFF6B6880),
    outlineVariant = Color(0xFFE7E4F0),
    error = Color(0xFFD32F2F),
)

private val KompisDark = darkColorScheme(
    primary = Color(0xFFB9ABFF),
    onPrimary = Color(0xFF1F1650),
    primaryContainer = Color(0xFF2E2760),
    onPrimaryContainer = Color(0xFFE6E0FF),
    secondary = Color(0xFF5EEAD4),
    background = Color(0xFF0D0C14),
    onBackground = Color(0xFFECEAF4),
    surface = Color(0xFF0D0C14),
    onSurface = Color(0xFFECEAF4),
    surfaceContainer = Color(0xFF1A1825),
    surfaceVariant = Color(0xFF26233A),
    onSurfaceVariant = Color(0xFFA39FB8),
    outlineVariant = Color(0xFF2C2940),
    error = Color(0xFFFF8A80),
)

@Composable
fun KompisTheme(dark: Boolean = LocalDarkTheme.current, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) KompisDark else KompisLight,
        typography = MaterialTheme.typography,
        content = content,
    )
}
