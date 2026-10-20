import Foundation
import YouTubeKit

/// Finner lyden til en YouTube-video med YouTubeKit. Ligger i en egen fil så navnene i YouTubeKit
/// ikke kolliderer med de fra Kotlin.
enum EkkoYouTube {
    /// Adressen til lydsporet, filendelsen og tittelen på videoen.
    static func audioStream(videoId: String) async throws -> (url: URL, fileExtension: String, title: String) {
        // Lokalt først. Har YouTube endret noe som YouTubeKit ikke har fått med seg ennå, hjelper serveren til YouTubeKit.
        let video = YouTube(videoID: videoId, methods: [.local, .remote])
        let streams: [YouTubeKit.Stream]
        do {
            streams = try await video.streams
        } catch let error as YouTubeKitError {
            throw EkkoError(message(for: error))
        }
        // Helst bare lyd i .m4a. Ellers en liten video med lyd, som lyden hentes ut av.
        let audio = streams.filterAudioOnly().filter { $0.fileExtension == .m4a }.highestAudioBitrateStream()
        let withVideo = streams.filterVideoAndAudio().filter { $0.isNativelyPlayable }.lowestResolutionStream()
        guard let stream = audio ?? withVideo else {
            throw EkkoError("Fant ingen lyd YouTube lar oss spille av i denne videoen.")
        }
        let title = (try? await video.metadata)?.title ?? ""
        return (stream.url, stream.fileExtension.rawValue, title)
    }

    private static func message(for error: YouTubeKitError) -> String {
        switch error {
        case .videoPrivate: return "Videoen er privat."
        case .videoAgeRestricted: return "Videoen er aldersbegrenset, så den kan ikke hentes."
        case .membersOnly: return "Videoen er bare for medlemmer av kanalen."
        case .videoRegionBlocked: return "Videoen er ikke tilgjengelig i Norge."
        case .liveStreamError: return "Direktesendinger kan ikke hentes."
        case .videoUnavailable: return "Videoen finnes ikke lenger."
        default: return "Klarte ikke å hente lyden fra YouTube. Prøv igjen litt senere."
        }
    }
}
