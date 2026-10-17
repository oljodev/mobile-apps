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
            // SideStore kan ha fornyet appen siden sist, så flytt påminnelsen til ny utløpsdato.
            if phase == .active {
                Notifications.shared.scheduleExpiryReminder()
            }
        }
    }
}
