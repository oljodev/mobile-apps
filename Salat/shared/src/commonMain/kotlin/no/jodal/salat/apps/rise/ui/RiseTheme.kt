package no.jodal.salat.apps.rise.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import no.jodal.salat.ui.LocalDarkTheme

/** Rise har sitt eget uttrykk: soloppgang, med oransje som hovedfarge. */
object RisePalette {
    val Gold = Color(0xFFFFC46B)
    val White = Color.White

    /** Natt som går over i soloppgang, brukt øverst i Rise. */
    fun sunrise(darkTheme: Boolean): Brush = Brush.verticalGradient(
        if (darkTheme) {
            listOf(Color(0xFF0E0C24), Color(0xFF2A1F52), Color(0xFF7A3A55), Color(0xFFC75B3C))
        } else {
            listOf(Color(0xFF29235E), Color(0xFF6B3F86), Color(0xFFE0674A), Color(0xFFF7A35C))
        },
    )
}

private val RiseLight = lightColorScheme(
    primary = Color(0xFFE5542A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0D2),
    onPrimaryContainer = Color(0xFF5A1A00),
    background = Color(0xFFF5F3F0),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFF5F3F0),
    onSurface = Color(0xFF1C1B1F),
    surfaceContainer = Color.White,
    surfaceVariant = Color(0xFFECE8E3),
    onSurfaceVariant = Color(0xFF6E6862),
    outlineVariant = Color(0xFFE6E1DB),
    error = Color(0xFFD32F2F),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val RiseDark = darkColorScheme(
    primary = Color(0xFFFF8A5C),
    onPrimary = Color(0xFF3A1200),
    primaryContainer = Color(0xFF5C2410),
    onPrimaryContainer = Color(0xFFFFDBCC),
    background = Color(0xFF0F0E17),
    onBackground = Color(0xFFEDEAF5),
    surface = Color(0xFF0F0E17),
    onSurface = Color(0xFFEDEAF5),
    surfaceContainer = Color(0xFF1C1A27),
    surfaceVariant = Color(0xFF282637),
    onSurfaceVariant = Color(0xFFA8A4BB),
    outlineVariant = Color(0xFF2E2C3F),
    error = Color(0xFFFF8A80),
    errorContainer = Color(0xFF5C1A1A),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun RiseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (LocalDarkTheme.current) RiseDark else RiseLight,
        typography = MaterialTheme.typography,
        content = content,
    )
}
