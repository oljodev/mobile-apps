package no.jodal.salat.apps

import no.jodal.salat.MiniApp
import no.jodal.salat.apps.rise.ui.RiseScreen
import no.jodal.salat.resources.Res
import no.jodal.salat.resources.rise_logo
import no.jodal.salat.ui.SalatColors

val Rise = MiniApp(
    title = "Rise",
    emoji = "🍚",
    description = "Vekking, trening og påminnelser",
    icon = Res.drawable.rise_logo,
    color = SalatColors.Lemon,
    content = { bridge -> RiseScreen(bridge) },
)
