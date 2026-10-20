import Foundation
import Shared
import WidgetKit

/// Det widgetene får appen til å gjøre.
enum WidgetActions {
    @MainActor
    static func playEkko(_ soundId: String) {
        EkkoWidgetsKt.playFromWidget(bridge: IosNativeBridge.shared, clipId: soundId)
    }

    static func open(_ url: String) {
        DeepLinksKt.openDeepLink(url: url)
    }

    /// Åpner en lenke en widget la igjen mens appen ikke kjørte.
    static func openPendingLink() {
        guard let data = SharedFiles.read("pending-link"), let url = String(data: data, encoding: .utf8) else { return }
        SharedFiles.remove("pending-link")
        open(url)
    }

    /// Lagrer det widgetene skal vise, og ber dem tegne seg på nytt hvis noe er endret.
    static func save(_ name: String, _ data: Data) {
        if SharedFiles.write(name, data) {
            WidgetCenter.shared.reloadAllTimelines()
        }
    }
}
