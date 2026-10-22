import AppIntents
import Shared

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
        if BremsStoreKt.bremsOpened(bridge: IosNativeBridge.shared, app: app) {
            WidgetActions.open("salat://brems/pause")
            try await continueInForeground(alwaysConfirm: false)
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
