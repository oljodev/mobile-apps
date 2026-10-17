import Shared
import SwiftUI

/// Viser Compose-UI-et fra Kotlin.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(bridge: IosNativeBridge())
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
