package no.jodal.salat.apps

import androidx.compose.ui.graphics.Color
import no.jodal.salat.MiniApp
import no.jodal.salat.MiniAppIcon
import no.jodal.salat.apps.brems.ui.BremsScreen
import no.jodal.salat.resources.Res
import no.jodal.salat.resources.brems_logo

val Brems = MiniApp(
    title = "Brems",
    description = "Pauser og grenser for appene du scroller i",
    icon = MiniAppIcon.Logo(Res.drawable.brems_logo, fullBleed = true),
    color = Color(0xFF5B4FF0),
    customTopBar = true,
    content = { BremsScreen(it.bridge, onBack = it.onBack) },
)
