import Foundation

/// Leser signeringsfilen SideStore legger inn i appen.
enum ProvisioningProfile {
    /// Når signeringen går ut, eller `nil` hvis appen ikke er installert med en signeringsfil.
    static let expirationDate: Date? = {
        guard
            let url = Bundle.main.url(forResource: "embedded", withExtension: "mobileprovision"),
            let data = try? Data(contentsOf: url),
            // Filen er en signert plist. Selve plist-en ligger som klartekst inni.
            let start = data.range(of: Data("<?xml".utf8)),
            let end = data.range(of: Data("</plist>".utf8), in: start.lowerBound..<data.endIndex),
            let plist = try? PropertyListSerialization.propertyList(
                from: data.subdata(in: start.lowerBound..<end.upperBound),
                format: nil
            ) as? [String: Any]
        else { return nil }
        return plist["ExpirationDate"] as? Date
    }()
}
