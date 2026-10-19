package no.jodal.salat.apps

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import no.jodal.salat.MiniApp
import no.jodal.salat.MiniAppIcon
import no.jodal.salat.ui.SalatColors

// Mini-apper som er planlagt, men ikke laget ennå.

val Kompis = MiniApp(
    title = "Kompis",
    description = "Snakk med en AI, med stemmen",
    icon = MiniAppIcon.Symbol(Icons.Rounded.Mic),
    color = SalatColors.Lavender,
)
