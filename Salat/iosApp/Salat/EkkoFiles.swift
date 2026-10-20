import Foundation

/// Hvor lydfilene i Ekko ligger: én `.m4a`-fil per lyd, med lyden sin id som navn.
enum EkkoFiles {
    static var folder: URL {
        let url = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("Ekko", isDirectory: true)
        try? FileManager.default.createDirectory(at: url, withIntermediateDirectories: true)
        return url
    }

    static func audio(_ clipId: String) -> URL {
        folder.appendingPathComponent("\(clipId).m4a")
    }

    static func delete(_ clipId: String) {
        try? FileManager.default.removeItem(at: audio(clipId))
    }

    /// En ny, tom fil i den midlertidige mappen.
    static func temporary(extension ext: String) -> URL {
        FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString)
            .appendingPathExtension(ext.isEmpty ? "mp4" : ext)
    }
}

/// En feil med en melding som kan vises rett til brukeren.
struct EkkoError: LocalizedError {
    let message: String

    init(_ message: String) {
        self.message = message
    }

    var errorDescription: String? { message }
}
