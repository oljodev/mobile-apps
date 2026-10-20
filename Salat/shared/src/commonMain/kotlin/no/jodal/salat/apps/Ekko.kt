package no.jodal.salat.apps

import no.jodal.salat.MiniApp
import no.jodal.salat.MiniAppIcon
import no.jodal.salat.apps.ekko.ui.EkkoScreen
import no.jodal.salat.resources.Res
import no.jodal.salat.resources.ekko_logo
import no.jodal.salat.ui.SalatColors

val Ekko = MiniApp(
    title = "Ekko",
    description = "Lyder og sitater fra korte videoer",
    icon = MiniAppIcon.Logo(Res.drawable.ekko_logo, fullBleed = true),
    color = SalatColors.Leaf,
    customTopBar = true,
    content = { EkkoScreen(it.bridge, onBack = it.onBack) },
)
