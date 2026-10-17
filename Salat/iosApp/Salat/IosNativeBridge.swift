import Foundation
import Shared

/// Swift-implementasjonen av `NativeBridge` fra Kotlin.
final class IosNativeBridge: NSObject, NativeBridge {
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
}
