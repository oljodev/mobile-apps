import AppIntents

/// Kjøres når en alarm med «Sjekk at jeg er oppe» stoppes, og planlegger en ny alarm om 5 minutter.
struct AwakeCheckIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Sjekk at jeg er oppe"
    static var isDiscoverable: Bool = false

    @Parameter(title: "Alarm")
    var alarmTitle: String

    init() {}

    init(alarmTitle: String) {
        self.alarmTitle = alarmTitle
    }

    func perform() async throws -> some IntentResult {
        await Alarms.shared.startAwakeCheck(title: alarmTitle)
        return .result()
    }
}
