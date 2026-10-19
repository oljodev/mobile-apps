import ActivityKit
import AlarmKit
import SwiftUI

/// Ekstra informasjon som følger med hver alarm. Tom foreløpig.
struct RiseMetadata: AlarmMetadata {}

/// Alarmer som ringer gjennom lydløs og Fokus, med AlarmKit.
final class Alarms {
    static let shared = Alarms()

    /// Planlegger en testalarm. `completion` kalles på hovedtråden med en feilmelding, eller `nil` hvis det gikk bra.
    func scheduleTestAlarm(after seconds: TimeInterval, snoozeMinutes: Int, completion: @escaping (String?) -> Void) {
        Task { @MainActor in
            do {
                guard try await requestAuthorization() else {
                    completion("Salat har ikke lov til å lage alarmer. Slå det på i Innstillinger → Salat.")
                    return
                }
                let snooze = snoozeMinutes > 0
                let alert = AlarmPresentation.Alert(
                    title: "Testalarm fra Rise",
                    stopButton: AlarmButton(text: "Stopp", textColor: .white, systemImageName: "stop.circle"),
                    secondaryButton: snooze ? AlarmButton(text: "Slumre", textColor: .white, systemImageName: "zzz") : nil,
                    secondaryButtonBehavior: snooze ? .countdown : nil
                )
                let attributes = AlarmAttributes(
                    presentation: AlarmPresentation(alert: alert),
                    metadata: RiseMetadata(),
                    tintColor: Color.orange
                )
                let configuration = AlarmManager.AlarmConfiguration(
                    countdownDuration: snooze
                        ? Alarm.CountdownDuration(preAlert: nil, postAlert: TimeInterval(snoozeMinutes * 60))
                        : nil,
                    schedule: .fixed(Date().addingTimeInterval(seconds)),
                    attributes: attributes,
                    stopIntent: nil,
                    secondaryIntent: nil,
                    sound: .default
                )
                _ = try await AlarmManager.shared.schedule(id: UUID(), configuration: configuration)
                completion(nil)
            } catch {
                completion("Kunne ikke lage alarmen: \(error)")
            }
        }
    }

    private func requestAuthorization() async throws -> Bool {
        switch AlarmManager.shared.authorizationState {
        case .authorized:
            return true
        case .denied:
            return false
        case .notDetermined:
            return try await AlarmManager.shared.requestAuthorization() == .authorized
        @unknown default:
            return false
        }
    }
}
