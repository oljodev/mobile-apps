import UserNotifications

final class Notifications: NSObject, UNUserNotificationCenterDelegate {
    static let shared = Notifications()

    private let center = UNUserNotificationCenter.current()
    private let expiryReminderId = "salat.expiry-reminder"

    /// Ber om tillatelse til varsler og planlegger påminnelsen om fornyelse.
    func start() {
        center.delegate = self
        center.requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
            if granted {
                self.scheduleExpiryReminder()
            }
        }
    }

    /// Planlegger et varsel ett døgn før signeringen går ut. Erstatter et tidligere planlagt varsel.
    func scheduleExpiryReminder() {
        guard let expiry = ProvisioningProfile.expirationDate else { return }
        let fireDate = expiry.addingTimeInterval(-24 * 60 * 60)
        guard fireDate > Date() else { return }

        let content = UNMutableNotificationContent()
        content.title = "Salat går ut i morgen"
        content.body = "Åpne SideStore og trykk «Refresh All» for å fornye appene dine."
        content.sound = .default

        let components = Calendar.current.dateComponents(
            [.year, .month, .day, .hour, .minute, .second],
            from: fireDate
        )
        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
        center.add(UNNotificationRequest(identifier: expiryReminderId, content: content, trigger: trigger))
    }

    func sendTestNotification(after seconds: TimeInterval) {
        let content = UNMutableNotificationContent()
        content.title = "Hei fra Salat 🥗"
        content.body = "Varsler virker!"
        content.sound = .default

        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: max(seconds, 1), repeats: false)
        center.add(UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: trigger))
    }

    /// Vis varsler også når appen er åpen.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .list, .sound])
    }
}
