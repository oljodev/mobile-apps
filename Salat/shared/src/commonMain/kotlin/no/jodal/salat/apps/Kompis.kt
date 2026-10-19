package no.jodal.salat.apps

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import no.jodal.salat.MiniApp
import no.jodal.salat.MiniAppIcon
import no.jodal.salat.apps.kompis.ui.KompisScreen
import no.jodal.salat.ui.SalatColors

val Kompis = MiniApp(
    title = "Kompis",
    description = "Snakk med en AI, med stemmen",
    icon = MiniAppIcon.Symbol(Icons.Rounded.Mic),
    color = SalatColors.Lavender,
    customTopBar = true,
    content = { KompisScreen(it.bridge, onBack = it.onBack) },
)
