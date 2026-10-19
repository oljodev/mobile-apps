import Shared
import SwiftUI

@main
struct SalatApp: App {
    @Environment(\.scenePhase) private var scenePhase

    init() {
        Notifications.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            ComposeView().ignoresSafeArea()
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                // SideStore kan ha fornyet appen siden sist, så flytt påminnelsen til ny utløpsdato.
                Notifications.shared.scheduleExpiryReminder()
                // Fyll på alarmer i Rise og se etter «Jeg er oppe».
                RiseStoreKt.refreshRise(bridge: IosNativeBridge.shared)
            }
        }
    }
}
