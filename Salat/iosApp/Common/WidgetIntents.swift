import AppIntents
import Foundation

// Disse ligger både i appen og i widgetene. Det som faktisk skjer, skjer i appen: der finnes
// lydfilene og Kotlin-koden. I widgetene er `WIDGET_EXTENSION` satt.

/// Spiller en lyd fra Ekko uten å åpne appen. Som lyd-handling kjører den i appen, i bakgrunnen.
struct PlaySoundIntent: AudioPlaybackIntent {
    static let title: LocalizedStringResource = "Spill lyd fra Ekko"
    static let description = IntentDescription("Spiller en lyd fra Ekko uten å åpne appen.")
    static let isDiscoverable = false

    @Parameter(title: "Lyd")
    var soundId: String

    init() {}

    init(soundId: String) {
        self.soundId = soundId
    }

    @MainActor
    func perform() async throws -> some IntentResult {
        #if !WIDGET_EXTENSION
        WidgetActions.playEkko(soundId)
        #endif
        return .result()
    }
}

/// Åpner Kompis og starter en samtale.
struct StartKompisIntent: AppIntent {
    static let title: LocalizedStringResource = "Snakk med Kompis"
    static let description = IntentDescription("Åpner Kompis og starter en samtale.")
    static let openAppWhenRun = true

    init() {}

    @MainActor
    func perform() async throws -> some IntentResult {
        #if WIDGET_EXTENSION
        // Kjører den i widgetene, legges lenken igjen til appen, som tar den når den åpnes.
        SharedFiles.write("pending-link", Data("salat://kompis/start".utf8))
        #else
        WidgetActions.open("salat://kompis/start")
        #endif
        return .result()
    }
}

/// En lyd i Ekko, så man kan velge hvilken lyd widgeten på låseskjermen skal spille.
struct SoundEntity: AppEntity {
    static let typeDisplayRepresentation: TypeDisplayRepresentation = "Lyd"
    static let defaultQuery = SoundQuery()

    let id: String
    let title: String
    let artist: String

    var displayRepresentation: DisplayRepresentation {
        artist.isEmpty
            ? DisplayRepresentation(title: "\(title)")
            : DisplayRepresentation(title: "\(title)", subtitle: "\(artist)")
    }
}

struct SoundQuery: EntityQuery {
    func entities(for identifiers: [String]) async throws -> [SoundEntity] {
        sounds().filter { identifiers.contains($0.id) }
    }

    func suggestedEntities() async throws -> [SoundEntity] {
        sounds()
    }

    private func sounds() -> [SoundEntity] {
        (EkkoBoardData.load()?.sounds ?? []).map { SoundEntity(id: $0.id, title: $0.title, artist: $0.artist) }
    }
}

/// Innstillingen til widgeten på låseskjermen: hvilken lyd den spiller.
struct ChooseSoundIntent: WidgetConfigurationIntent {
    static let title: LocalizedStringResource = "Velg lyd"
    static let description = IntentDescription("Lyden som spilles når du trykker på widgeten.")

    @Parameter(title: "Lyd")
    var sound: SoundEntity?

    init() {}
}
