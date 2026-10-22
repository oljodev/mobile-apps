import AppIntents
import Shared
import UIKit

// Handlingene automasjonene i Snarveier kjører, så Brems vet når apper åpnes og lukkes.

/// Kjøres når en app åpnes. Er det tid for en pause, åpnes Brems med pausen.
struct BremsOpenedIntent: AppIntent {
    static let title: LocalizedStringResource = "Brems: App åpnet"
    static let description = IntentDescription(
        "Legg denne i en automasjon som kjører når en app åpnes. Brems viser en pause når det er tid for det."
    )
    // Starter i bakgrunnen, og hopper fram bare når det skal være pause.
    static let supportedModes: IntentModes = [.background, .foreground(.dynamic)]

    @Parameter(title: "App", description: "Navnet på appen, f.eks. TikTok. Kan stå tomt.")
    var app: String?

    init() {}

    @MainActor
    func perform() async throws -> some IntentResult {
        let bridge = IosNativeBridge.shared
        guard BremsStoreKt.bremsOpened(bridge: bridge, app: app) else { return .result() }
        WidgetActions.open("salat://brems/pause")
        if UIApplication.shared.applicationState == .active {
            BremsStoreKt.bremsLog(bridge: bridge, text: "Brems var alt framme")
            return .result()
        }
        do {
            try await continueInForeground(alwaysConfirm: false)
            BremsStoreKt.bremsLog(bridge: bridge, text: "Brems kom fram")
        } catch {
            // Slipper iOS ikke Brems fram, kommer i det minste et varsel.
            BremsStoreKt.bremsLog(bridge: bridge, text: "Fikk ikke åpne Brems: \(error.localizedDescription)")
            Notifications.shared.sendBremsNow(title: "Pause først", body: "Trykk her for å ta pausen i Brems.")
        }
        return .result()
    }
}

/// Kjøres når en app lukkes, så Brems vet hvor lenge den var åpen.
struct BremsClosedIntent: AppIntent {
    static let title: LocalizedStringResource = "Brems: App lukket"
    static let description = IntentDescription("Legg denne i en automasjon som kjører når en app lukkes.")

    init() {}

    @MainActor
    func perform() async throws -> some IntentResult {
        BremsStoreKt.bremsClosed(bridge: IosNativeBridge.shared)
        return .result()
    }
}
