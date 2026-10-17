package no.jodal.salat

import androidx.compose.runtime.Composable
import no.jodal.salat.apps.Fornyelse
import no.jodal.salat.apps.Varseltest

/** En «app inni appen» som vises på forsiden. */
class MiniApp(
    val title: String,
    val emoji: String,
    val description: String,
    val content: @Composable (NativeBridge) -> Unit,
)

/** Alle mini-appene, i den rekkefølgen de vises på forsiden. Nye mini-apper legges til her. */
val miniApps = listOf(
    Fornyelse,
    Varseltest,
)
