package no.jodal.salat

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** Kalles fra Swift for å vise Compose-UI-et. */
fun MainViewController(bridge: NativeBridge): UIViewController =
    ComposeUIViewController { SalatApp(bridge) }
