import Foundation
import Shared

/// Swift-implementasjonen av `NativeBridge` fra Kotlin.
final class IosNativeBridge: NSObject, NativeBridge {
    static let shared = IosNativeBridge()

    func signingExpiresAtMillis() -> KotlinLong? {
        guard let expiry = ProvisioningProfile.expirationDate else { return nil }
        return KotlinLong(value: Int64(expiry.timeIntervalSince1970 * 1000))
    }

    func scheduleExpiryReminder() {
        Notifications.shared.scheduleExpiryReminder()
    }

    func sendTestNotification(delaySeconds: Int32) {
        Notifications.shared.sendTestNotification(after: TimeInterval(delaySeconds))
    }

    func scheduleTestAlarm(delaySeconds: Int32, snoozeMinutes: Int32, onResult: @escaping (String?) -> Void) {
        Alarms.shared.scheduleTestAlarm(
            after: TimeInterval(delaySeconds),
            snoozeMinutes: Int(snoozeMinutes),
            completion: onResult
        )
    }

    func loadText(key: String) -> String? {
        UserDefaults.standard.string(forKey: "salat.\(key)")
    }

    func saveText(key: String, value: String) {
        UserDefaults.standard.set(value, forKey: "salat.\(key)")
    }

    func applyRiseSchedule(planJson: String, onResult: @escaping (String?) -> Void) {
        RiseScheduler.apply(planJson: planJson, completion: onResult)
    }

    func previewSound(sound: String) {
        SoundPreview.shared.play(sound)
    }

    func stopSoundPreview() {
        SoundPreview.shared.stop()
    }
}
