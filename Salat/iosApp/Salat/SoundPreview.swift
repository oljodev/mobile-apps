import AudioToolbox
import AVFoundation

/// Spiller av lydene i Rise, så man kan høre dem før man velger.
final class SoundPreview {
    static let shared = SoundPreview()

    private var player: AVAudioPlayer?

    func play(_ sound: String) {
        stop()
        guard sound != "standard", let url = Bundle.main.url(forResource: sound, withExtension: "wav") else {
            AudioServicesPlayAlertSound(SystemSoundID(1005))
            return
        }
        try? AVAudioSession.sharedInstance().setCategory(.playback)
        try? AVAudioSession.sharedInstance().setActive(true)
        player = try? AVAudioPlayer(contentsOf: url)
        player?.play()
    }

    func stop() {
        player?.stop()
        player = nil
    }
}
