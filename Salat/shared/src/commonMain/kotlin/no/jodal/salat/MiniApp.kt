package no.jodal.salat

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.DrawableResource
import no.jodal.salat.apps.AiPrat
import no.jodal.salat.apps.Alarmer
import no.jodal.salat.apps.Fornyelse
import no.jodal.salat.apps.Varseltest

/** En «app inni appen» som vises på forsiden. */
class MiniApp(
    val title: String,
    val emoji: String,
    val description: String,
    /** Bilde som brukes i stedet for [emoji] på forsiden, hvis mini-appen har en egen logo. */
    val icon: DrawableResource? = null,
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
    Alarmer,
    AiPrat,
    Fornyelse,
    Varseltest,
)
