import ActivityKit
import AlarmKit
import CryptoKit
import SwiftUI

/// Ekstra informasjon som følger med hver alarm. Tom foreløpig.
struct RiseMetadata: AlarmMetadata {}

/// Alarmer som ringer gjennom lydløs og Fokus, med AlarmKit.
final class Alarms {
    static let shared = Alarms()

    /// Hvor lenge etter at en vekkealarm er stoppet Rise ringer igjen, hvis man ikke har trykket «Jeg er oppe».
    static let awakeCheckDelay: TimeInterval = 5 * 60

    private let manager = AlarmManager.shared
    private let defaults = UserDefaults.standard
    private let awakeTitleKey = "rise.awake.title"
    private let awakeBackupKey = "rise.awake.backup"

    /// Gjør de planlagte alarmene lik `items`.
    ///
    /// Alarmer som ringer eller slumrer akkurat nå, og reservealarmen for «Jeg er oppe», får være i fred.
    func apply(_ items: [PlannedItem]) async throws {
        let existing = try manager.alarms
        if items.isEmpty && existing.isEmpty { return }
        guard try await requestAuthorization() else { throw RiseError.alarmsNotAllowed }

        var wanted: [UUID: PlannedItem] = [:]
        for item in items {
            wanted[Self.uuid(for: item)] = item
        }
        let backupID = awakeBackupID
        for alarm in existing where alarm.state == .scheduled && wanted[alarm.id] == nil && alarm.id != backupID {
            try manager.cancel(id: alarm.id)
        }
        let existingIDs = Set(existing.map(\.id))
        for (id, item) in wanted where !existingIDs.contains(id) {
            let configuration = makeConfiguration(
                title: item.title,
                date: item.fireAt,
                sound: item.sound,
                snoozeMinutes: item.snoozeMinutes,
                awakeCheckTitle: item.awakeCheck ? item.title : nil
            )
            _ = try await manager.schedule(id: id, configuration: configuration)
        }
    }

    // MARK: - «Jeg er oppe»

    /// Tittelen på vekkealarmen som venter på «Jeg er oppe», eller `nil`.
    var pendingAwakeTitle: String? {
        defaults.string(forKey: awakeTitleKey)
    }

    /// Kalles når en alarm med «Sjekk at jeg er oppe» stoppes. Planlegger en ny alarm om litt.
    func startAwakeCheck(title: String) async {
        defaults.set(title, forKey: awakeTitleKey)
        let id = UUID()
        defaults.set(id.uuidString, forKey: awakeBackupKey)
        let configuration = makeConfiguration(
            title: "Er du oppe? ☀️",
            date: Date().addingTimeInterval(Self.awakeCheckDelay),
            sound: "rise-kraftig",
            snoozeMinutes: 0,
            awakeCheckTitle: title
        )
        _ = try? await manager.schedule(id: id, configuration: configuration)
    }

    func confirmAwake() {
        let backupID = awakeBackupID
        defaults.removeObject(forKey: awakeTitleKey)
        defaults.removeObject(forKey: awakeBackupKey)
        guard let backupID else { return }
        try? manager.cancel(id: backupID)
    }

    private var awakeBackupID: UUID? {
        defaults.string(forKey: awakeBackupKey).flatMap(UUID.init(uuidString:))
    }

    // MARK: - Test

    /// Planlegger en testalarm. `completion` kalles på hovedtråden med en feilmelding, eller `nil` hvis det gikk bra.
    func scheduleTestAlarm(after seconds: TimeInterval, snoozeMinutes: Int, completion: @escaping (String?) -> Void) {
        Task { @MainActor in
            do {
                guard try await requestAuthorization() else { throw RiseError.alarmsNotAllowed }
                let configuration = makeConfiguration(
                    title: "Testalarm fra Rise",
                    date: Date().addingTimeInterval(seconds),
                    sound: "standard",
                    snoozeMinutes: snoozeMinutes,
                    awakeCheckTitle: nil
                )
                _ = try await manager.schedule(id: UUID(), configuration: configuration)
                completion(nil)
            } catch {
                completion(error.localizedDescription)
            }
        }
    }

    // MARK: - Hjelpere

    private func makeConfiguration(
        title: String,
        date: Date,
        sound: String,
        snoozeMinutes: Int,
        awakeCheckTitle: String?
    ) -> AlarmManager.AlarmConfiguration<RiseMetadata> {
        let snooze = snoozeMinutes > 0
        let alert = AlarmPresentation.Alert(
            title: LocalizedStringResource(stringLiteral: title),
            stopButton: AlarmButton(text: "Stopp", textColor: .white, systemImageName: "stop.circle"),
            secondaryButton: snooze ? AlarmButton(text: "Slumre", textColor: .white, systemImageName: "zzz") : nil,
            secondaryButtonBehavior: snooze ? .countdown : nil
        )
        let attributes = AlarmAttributes(
            presentation: AlarmPresentation(alert: alert),
            metadata: RiseMetadata(),
            tintColor: Color.orange
        )
        return AlarmManager.AlarmConfiguration(
            countdownDuration: snooze
                ? Alarm.CountdownDuration(preAlert: nil, postAlert: TimeInterval(snoozeMinutes * 60))
                : nil,
            schedule: .fixed(date),
            attributes: attributes,
            stopIntent: awakeCheckTitle.map { AwakeCheckIntent(alarmTitle: $0) },
            secondaryIntent: nil,
            sound: sound == "standard" ? .default : .named("\(sound).wav")
        )
    }

    private func requestAuthorization() async throws -> Bool {
        switch manager.authorizationState {
        case .authorized:
            return true
        case .denied:
            return false
        case .notDetermined:
            return try await manager.requestAuthorization() == .authorized
        @unknown default:
            return false
        }
    }

    /// En fast ID for hver planlagte alarm, så den ikke planlegges på nytt hvis ingenting er endret.
    private static func uuid(for item: PlannedItem) -> UUID {
        let key = "\(item.id)|\(item.title)|\(item.sound)|\(item.snoozeMinutes)|\(item.awakeCheck)"
        let b = Array(Insecure.MD5.hash(data: Data(key.utf8)))
        return UUID(uuid: (b[0], b[1], b[2], b[3], b[4], b[5], b[6], b[7], b[8], b[9], b[10], b[11], b[12], b[13], b[14], b[15]))
    }
}
