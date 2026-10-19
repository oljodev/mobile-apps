import ActivityKit
import AlarmKit
import CryptoKit
import SwiftUI

/// Ekstra informasjon som følger med hver alarm. Tom foreløpig.
struct RiseMetadata: AlarmMetadata {}

/// Alarmer som ringer gjennom lydløs og Fokus, med AlarmKit.
final class Alarms {
    static let shared = Alarms()

    private let manager = AlarmManager.shared

    /// Hvilken alarm i Rise hver AlarmKit-alarm hører til, så slumring kan stoppes når alarmen slettes.
    private var owners: [String: String] {
        get { UserDefaults.standard.dictionary(forKey: "rise.alarmOwners") as? [String: String] ?? [:] }
        set { UserDefaults.standard.set(newValue, forKey: "rise.alarmOwners") }
    }

    /// Gjør de planlagte alarmene lik `items`.
    ///
    /// Alarmer som ringer eller slumrer akkurat nå, får være i fred, så lenge alarmen de hører til
    /// fortsatt finnes og er på (`activeAlarmIds`).
    func apply(_ items: [PlannedItem], activeAlarmIds: Set<String>) async throws {
        let existing = try manager.alarms
        if items.isEmpty && existing.isEmpty { return }
        guard try await requestAuthorization() else { throw RiseError.alarmsNotAllowed }

        var wanted: [UUID: PlannedItem] = [:]
        for item in items {
            wanted[Self.uuid(for: item)] = item
        }
        var owners = self.owners
        for (id, item) in wanted {
            owners[id.uuidString] = item.alarmId
        }
        for alarm in existing where wanted[alarm.id] == nil {
            let isScheduled = alarm.state == .scheduled
            let ownerIsGone = owners[alarm.id.uuidString].map { !activeAlarmIds.contains($0) } ?? false
            // Planlagte alarmer som ikke trengs lenger, og slumring eller ringing fra alarmer som er slettet eller skrudd av.
            if isScheduled || ownerIsGone {
                try manager.cancel(id: alarm.id)
            }
        }
        let existingIDs = Set(existing.map(\.id))
        for (id, item) in wanted where !existingIDs.contains(id) {
            let configuration = makeConfiguration(
                title: item.title,
                date: item.fireAt,
                sound: item.sound,
                snoozeMinutes: item.snoozeMinutes
            )
            _ = try await manager.schedule(id: id, configuration: configuration)
        }
        let remaining = Set(try manager.alarms.map(\.id.uuidString))
        self.owners = owners.filter { remaining.contains($0.key) }
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
                    snoozeMinutes: snoozeMinutes
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
        snoozeMinutes: Int
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
            stopIntent: nil,
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
        let key = "\(item.id)|\(item.title)|\(item.sound)|\(item.snoozeMinutes)"
        let b = Array(Insecure.MD5.hash(data: Data(key.utf8)))
        return UUID(uuid: (b[0], b[1], b[2], b[3], b[4], b[5], b[6], b[7], b[8], b[9], b[10], b[11], b[12], b[13], b[14], b[15]))
    }
}
