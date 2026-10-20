import Foundation

/// Mappen appen og widgetene deler. Appen skriver hva widgetene skal vise, og widgetene leser det.
enum SharedFiles {
    /// SideStore gir app-gruppen et nytt navn når den signerer appen. Det riktige navnet står i
    /// signeringsfilen, og ofte også i `ALTAppGroups` i Info.plist.
    static let groupId: String = {
        if let group = ProvisioningProfile.applicationGroups.first(where: { !$0.contains("*") }) {
            return group
        }
        if let groups = Bundle.main.object(forInfoDictionaryKey: "ALTAppGroups") as? [String], let group = groups.first {
            return group
        }
        return "group.no.jodal.salat"
    }()

    static var folder: URL? {
        FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: groupId)
    }

    static func read(_ name: String) -> Data? {
        guard let url = folder?.appendingPathComponent("\(name).json") else { return nil }
        return try? Data(contentsOf: url)
    }

    /// Lagrer [data] som [name]. Gir `true` hvis noe er endret.
    @discardableResult
    static func write(_ name: String, _ data: Data) -> Bool {
        guard let url = folder?.appendingPathComponent("\(name).json") else { return false }
        if (try? Data(contentsOf: url)) == data { return false }
        try? data.write(to: url, options: .atomic)
        return true
    }

    static func remove(_ name: String) {
        guard let url = folder?.appendingPathComponent("\(name).json") else { return }
        try? FileManager.default.removeItem(at: url)
    }
}

/// Navnene på widgetene.
enum WidgetKind {
    static let soundboard = "ekko.soundboard"
    static let favoriteSound = "ekko.favorite"
    static let nextAlarm = "rise.next"
    static let kompis = "kompis.talk"
    static let kompisControl = "kompis.control"
}

/// Lydene i Ekko, slik Kotlin skriver dem (`EkkoWidgetData`).
struct EkkoBoardData: Codable {
    var sounds: [EkkoBoardSound]
    var soundboard: [String]

    static func load() -> EkkoBoardData? {
        SharedFiles.read("ekko").flatMap { try? JSONDecoder().decode(EkkoBoardData.self, from: $0) }
    }

    /// Lydene på lydbrettet, eller de nyeste lydene hvis lydbrettet er tomt.
    func board(_ count: Int) -> [EkkoBoardSound] {
        let chosen = soundboard.compactMap { id in sounds.first { $0.id == id } }
        return Array((chosen.isEmpty ? sounds : chosen).prefix(count))
    }
}

struct EkkoBoardSound: Codable, Identifiable {
    let id: String
    let title: String
    let artist: String
    /// De to fargene i omslaget, som `#RRGGBB`.
    let colors: [String]
    let waveform: [Double]
}

/// De neste alarmene i Rise.
struct RiseWidgetData: Codable {
    var alarms: [RiseWidgetAlarm]

    static func load() -> RiseWidgetData? {
        SharedFiles.read("rise").flatMap { try? JSONDecoder().decode(RiseWidgetData.self, from: $0) }
    }
}

struct RiseWidgetAlarm: Codable {
    let title: String
    let fireAtMillis: Int64

    var fireAt: Date {
        Date(timeIntervalSince1970: TimeInterval(fireAtMillis) / 1000)
    }
}
