import AppIntents
import SwiftUI
import WidgetKit

/// Fargene til Ekko: mørkt som en musikkspiller, med lime.
enum EkkoStyle {
    static let ink = Color(hex: "#0B0B0E")
    static let lime = Color(hex: "#C8F560")
    static let textDim = Color(hex: "#8E8E9A")
}

extension EkkoBoardSound {
    static let samples = [
        EkkoBoardSound(id: "1", title: "Det er ikke en bug", artist: "", colors: ["#FFD25E", "#FF8A3D"], waveform: [0.4, 0.7, 1, 0.6, 0.8, 0.5, 0.3]),
        EkkoBoardSound(id: "2", title: "Jeg er så lei av mandager", artist: "", colors: ["#5EE1F0", "#3A7BFF"], waveform: [0.6, 1, 0.5, 0.8, 0.4, 0.7, 0.5]),
        EkkoBoardSound(id: "3", title: "Nei, nei, nei", artist: "", colors: ["#7CF0B5", "#2FA8C9"], waveform: [0.3, 0.6, 0.9, 1, 0.6, 0.4, 0.3]),
        EkkoBoardSound(id: "4", title: "Det var det jeg sa!", artist: "", colors: ["#FF8A5B", "#E5487A"], waveform: [0.5, 0.8, 0.6, 1, 0.7, 0.9, 0.4]),
    ]
}

/// Lydbølgen i et omslag: korte, runde streker.
struct WaveBars: View {
    let levels: [Double]
    var barWidth: CGFloat = 3

    var body: some View {
        GeometryReader { geometry in
            HStack(alignment: .center, spacing: barWidth * 0.8) {
                ForEach(Array(levels.enumerated()), id: \.offset) { _, level in
                    Capsule().frame(width: barWidth, height: max(barWidth, geometry.size.height * min(max(level, 0.15), 1)))
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        }
    }
}

// MARK: Lydbrettet

struct SoundboardEntry: TimelineEntry {
    let date: Date
    let sounds: [EkkoBoardSound]
}

struct SoundboardProvider: TimelineProvider {
    func placeholder(in context: Context) -> SoundboardEntry {
        SoundboardEntry(date: .now, sounds: EkkoBoardSound.samples)
    }

    func getSnapshot(in context: Context, completion: @escaping (SoundboardEntry) -> Void) {
        completion(entry(for: context))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<SoundboardEntry>) -> Void) {
        // Appen ber om ny tidslinje når lydene endres.
        completion(Timeline(entries: [entry(for: context)], policy: .never))
    }

    private func entry(for context: Context) -> SoundboardEntry {
        let count = switch context.family {
        case .systemSmall: 4
        case .systemMedium: 8
        default: 12
        }
        let sounds = EkkoBoardData.load()?.board(count) ?? []
        return SoundboardEntry(date: .now, sounds: sounds.isEmpty && context.isPreview ? EkkoBoardSound.samples : sounds)
    }
}

struct SoundboardView: View {
    let entry: SoundboardEntry
    @Environment(\.widgetFamily) private var family

    private var columns: Int { family == .systemSmall ? 2 : family == .systemMedium ? 4 : 3 }
    private var rows: Int { family == .systemLarge ? 4 : 2 }

    var body: some View {
        if entry.sounds.isEmpty {
            VStack(spacing: 6) {
                Image("EkkoLogo").resizable().frame(width: 40, height: 40)
                Text("Ingen lyder ennå")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(.white)
                Text("Legg til lyder i Ekko")
                    .font(.system(size: 12))
                    .foregroundStyle(EkkoStyle.textDim)
            }
            .multilineTextAlignment(.center)
        } else {
            Grid(horizontalSpacing: 6, verticalSpacing: 6) {
                ForEach(0..<rows, id: \.self) { row in
                    GridRow {
                        ForEach(0..<columns, id: \.self) { column in
                            let index = row * columns + column
                            if index < entry.sounds.count {
                                SoundTile(sound: entry.sounds[index], compact: family == .systemMedium)
                            } else {
                                RoundedRectangle(cornerRadius: 14, style: .continuous)
                                    .fill(Color.white.opacity(0.04))
                            }
                        }
                    }
                }
            }
            .padding(8)
        }
    }
}

/// Én lyd på lydbrettet. Trykk for å spille den.
struct SoundTile: View {
    let sound: EkkoBoardSound
    var compact = false

    var body: some View {
        Button(intent: PlaySoundIntent(soundId: sound.id)) {
            VStack(alignment: .leading, spacing: 4) {
                WaveBars(levels: sound.waveform, barWidth: compact ? 2.5 : 3)
                    .frame(height: compact ? 14 : 20)
                    .foregroundStyle(EkkoStyle.ink.opacity(0.45))
                Spacer(minLength: 0)
                Text(sound.title)
                    .font(.system(size: compact ? 10 : 12, weight: .bold, design: .rounded))
                    .foregroundStyle(EkkoStyle.ink.opacity(0.85))
                    .lineLimit(2)
                    .minimumScaleFactor(0.85)
            }
            .padding(compact ? 7 : 9)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            .background(
                LinearGradient(
                    colors: sound.colors.map(Color.init(hex:)),
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                ),
                in: RoundedRectangle(cornerRadius: 14, style: .continuous)
            )
        }
        .buttonStyle(.plain)
    }
}

struct SoundboardWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: WidgetKind.soundboard, provider: SoundboardProvider()) { entry in
            SoundboardView(entry: entry)
                .containerBackground(EkkoStyle.ink, for: .widget)
                .widgetURL(URL(string: "salat://ekko"))
        }
        .configurationDisplayName("Ekko: Lydbrett")
        .description("Trykk på en lyd for å spille den. Velg lyder i Ekko med … og «Legg på lydbrettet».")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
        .contentMarginsDisabled()
    }
}

// MARK: Favoritt på låseskjermen

struct FavoriteSoundEntry: TimelineEntry {
    let date: Date
    let sound: EkkoBoardSound?
}

struct FavoriteSoundProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> FavoriteSoundEntry {
        FavoriteSoundEntry(date: .now, sound: EkkoBoardSound.samples[0])
    }

    func snapshot(for configuration: ChooseSoundIntent, in context: Context) async -> FavoriteSoundEntry {
        entry(for: configuration)
    }

    func timeline(for configuration: ChooseSoundIntent, in context: Context) async -> Timeline<FavoriteSoundEntry> {
        Timeline(entries: [entry(for: configuration)], policy: .never)
    }

    /// Lyden som er valgt, eller den første på lydbrettet.
    private func entry(for configuration: ChooseSoundIntent) -> FavoriteSoundEntry {
        let data = EkkoBoardData.load()
        let chosen = configuration.sound.flatMap { sound in data?.sounds.first { $0.id == sound.id } }
        return FavoriteSoundEntry(date: .now, sound: chosen ?? data?.board(1).first)
    }
}

struct FavoriteSoundView: View {
    let entry: FavoriteSoundEntry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        if let sound = entry.sound {
            Button(intent: PlaySoundIntent(soundId: sound.id)) {
                switch family {
                case .accessoryRectangular:
                    HStack(spacing: 8) {
                        ZStack {
                            Circle().fill(.white.opacity(0.2))
                            Image(systemName: "play.fill").font(.system(size: 14, weight: .bold))
                        }
                        .frame(width: 34, height: 34)
                        .widgetAccentable()
                        VStack(alignment: .leading, spacing: 1) {
                            Text(sound.title)
                                .font(.system(size: 15, weight: .semibold))
                                .lineLimit(2)
                            Text(sound.artist.isEmpty ? "Ekko" : sound.artist)
                                .font(.system(size: 12))
                                .foregroundStyle(.secondary)
                                .lineLimit(1)
                        }
                        Spacer(minLength: 0)
                    }
                default:
                    ZStack {
                        AccessoryWidgetBackground()
                        VStack(spacing: 3) {
                            Image(systemName: "play.fill").font(.system(size: 12, weight: .bold))
                            WaveBars(levels: sound.waveform, barWidth: 2.5)
                                .frame(width: 30, height: 12)
                        }
                        .widgetAccentable()
                    }
                }
            }
            .buttonStyle(.plain)
        } else {
            ZStack {
                if family == .accessoryCircular { AccessoryWidgetBackground() }
                Image(systemName: "waveform").font(.system(size: 18, weight: .semibold))
            }
            .widgetURL(URL(string: "salat://ekko"))
        }
    }
}

struct FavoriteSoundWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: WidgetKind.favoriteSound, intent: ChooseSoundIntent.self, provider: FavoriteSoundProvider()) { entry in
            FavoriteSoundView(entry: entry)
                .containerBackground(.clear, for: .widget)
        }
        .configurationDisplayName("Ekko: Favorittlyd")
        .description("Spill favorittlyden din rett fra låseskjermen. Hold inne for å velge lyd.")
        .supportedFamilies([.accessoryCircular, .accessoryRectangular])
    }
}
