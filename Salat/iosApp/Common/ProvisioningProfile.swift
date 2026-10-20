import Foundation

/// Leser signeringsfilen SideStore legger inn i appen og i widgetene.
enum ProvisioningProfile {
    /// Når signeringen går ut, eller `nil` hvis appen ikke er installert med en signeringsfil.
    static var expirationDate: Date? {
        plist?["ExpirationDate"] as? Date
    }

    /// App-gruppene appen har lov til å bruke, med navnene SideStore har gitt dem.
    static var applicationGroups: [String] {
        let entitlements = plist?["Entitlements"] as? [String: Any]
        return entitlements?["com.apple.security.application-groups"] as? [String] ?? []
    }

    private static let plist: [String: Any]? = {
        guard
            let url = Bundle.main.url(forResource: "embedded", withExtension: "mobileprovision"),
            let data = try? Data(contentsOf: url),
            // Filen er en signert plist. Selve plist-en ligger som klartekst inni.
            let start = data.range(of: Data("<?xml".utf8)),
            let end = data.range(of: Data("</plist>".utf8), in: start.lowerBound..<data.endIndex)
        else { return nil }
        return try? PropertyListSerialization.propertyList(
            from: data.subdata(in: start.lowerBound..<end.upperBound),
            format: nil
        ) as? [String: Any]
    }()
}
