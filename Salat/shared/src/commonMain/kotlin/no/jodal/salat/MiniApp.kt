package no.jodal.salat

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import no.jodal.salat.apps.Fornyelse
import no.jodal.salat.apps.Varseltest

/** En «app inni appen» som vises på forsiden. */
class MiniApp(
    val title: String,
    val emoji: String,
    val description: String,
    /** Fargen på kortet og toppen av mini-appen. */
    val color: Color,
    val section: Section = Section.Apps,
    /** Innholdet i mini-appen, eller `null` hvis den ikke er laget ennå. */
    val content: (@Composable (NativeBridge) -> Unit)? = null,
) {
    enum class Section { Apps, Tools }
}

/** Alle mini-appene, i den rekkefølgen de vises på forsiden. Nye mini-apper legges til her. */
val miniApps = listOf(
    Fornyelse,
    Varseltest,
)
