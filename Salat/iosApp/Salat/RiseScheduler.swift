import Foundation
import UserNotifications

/// Én alarm eller ett varsel fra Rise. Samme felter som `PlannedItem` i Kotlin.
struct PlannedItem: Decodable {
    let id: String
    let kind: String
    let fireAtMillis: Int64
    let title: String
    let body: String
    let sound: String
    let snoozeMinutes: Int

    var fireAt: Date {
        Date(timeIntervalSince1970: TimeInterval(fireAtMillis) / 1000)
    }
}

enum RiseError: LocalizedError {
    case alarmsNotAllowed

    var errorDescription: String? {
        switch self {
        case .alarmsNotAllowed:
            return "Salat har ikke lov til å lage alarmer. Slå det på i Innstillinger → Apper → Salat."
        }
    }
}

/// Planlegger alarmene og varslene fra Rise.
enum RiseScheduler {
    private static let notificationPrefix = "rise."

    static func apply(planJson: String, completion: @escaping (String?) -> Void) {
        Task { @MainActor in
            do {
                let now = Date()
                let items = try JSONDecoder()
                    .decode([PlannedItem].self, from: Data(planJson.utf8))
                    .filter { $0.fireAt > now }
                await applyNotifications(items.filter { $0.kind == "notification" })
                try await Alarms.shared.apply(items.filter { $0.kind == "alarm" })
                completion(nil)
            } catch {
                completion(error.localizedDescription)
            }
        }
    }

    private static func applyNotifications(_ items: [PlannedItem]) async {
        let center = UNUserNotificationCenter.current()
        let wanted = Set(items.map { notificationPrefix + $0.id })
        let stale = await center.pendingNotificationRequests()
            .map(\.identifier)
            .filter { $0.hasPrefix(notificationPrefix) && !wanted.contains($0) }
        center.removePendingNotificationRequests(withIdentifiers: stale)

        // Et varsel med samme ID erstattes, så endret tittel eller lyd kommer med.
        for item in items {
            let content = UNMutableNotificationContent()
            content.title = item.title
            content.body = item.body
            content.sound = item.sound == "standard"
                ? .default
                : UNNotificationSound(named: UNNotificationSoundName("\(item.sound).wav"))
            let components = Calendar.current.dateComponents(
                [.year, .month, .day, .hour, .minute, .second],
                from: item.fireAt
            )
            let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
            try? await center.add(UNNotificationRequest(
                identifier: notificationPrefix + item.id,
                content: content,
                trigger: trigger
            ))
        }
    }
}
