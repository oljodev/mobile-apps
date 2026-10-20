import AppIntents
import SwiftUI
import WidgetKit

/// Fargene til Kompis: svart, med et nordlys i turkis, blått, fiolett og rosa.
enum KompisStyle {
    static let aurora = [Color(hex: "#3DD6F5"), Color(hex: "#4F7CFF"), Color(hex: "#9B5CFF"), Color(hex: "#FF5CA8")]
    static let secondary = Color(hex: "#8E8E93")
}

/// De fem strekene fra logoen til Kompis.
struct KompisBars: View {
    var barWidth: CGFloat = 6
    var height: CGFloat = 34

    var body: some View {
        HStack(spacing: barWidth * 0.65) {
            ForEach(Array([0.4, 0.7, 1.0, 0.7, 0.4].enumerated()), id: \.offset) { _, level in
                Capsule().frame(width: barWidth, height: height * level)
            }
        }
    }
}

struct KompisEntry: TimelineEntry {
    let date: Date
}

struct KompisProvider: TimelineProvider {
    func placeholder(in context: Context) -> KompisEntry { KompisEntry(date: .now) }

    func getSnapshot(in context: Context, completion: @escaping (KompisEntry) -> Void) {
        completion(KompisEntry(date: .now))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<KompisEntry>) -> Void) {
        completion(Timeline(entries: [KompisEntry(date: .now)], policy: .never))
    }
}

struct KompisView: View {
    @Environment(\.widgetFamily) private var family

    var body: some View {
        if family == .accessoryCircular {
            ZStack {
                AccessoryWidgetBackground()
                KompisBars(barWidth: 3.5, height: 22).widgetAccentable()
            }
        } else {
            VStack(alignment: .leading, spacing: 0) {
                HStack(alignment: .top) {
                    KompisBars(barWidth: 5, height: 26)
                        .foregroundStyle(LinearGradient(colors: KompisStyle.aurora, startPoint: .leading, endPoint: .trailing))
                    Spacer()
                    Image(systemName: "mic.fill")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(width: 30, height: 30)
                        .background(.white.opacity(0.14), in: Circle())
                }
                Spacer(minLength: 0)
                Text("Snakk med")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(KompisStyle.secondary)
                Text("Kompis")
                    .font(.system(size: 24, weight: .bold))
                    .tracking(-0.5)
                    .foregroundStyle(.white)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        }
    }
}

/// Svart, med nordlyset som gløder nederst, som på samtaleskjermen. Ligger i bakgrunnen, så det
/// ikke påvirker hvor teksten havner.
struct KompisBackground: View {
    var body: some View {
        GeometryReader { geometry in
            Ellipse()
                .fill(LinearGradient(colors: KompisStyle.aurora, startPoint: .leading, endPoint: .trailing))
                .frame(width: geometry.size.width * 1.1, height: geometry.size.height * 0.5)
                .blur(radius: 30)
                .opacity(0.45)
                .position(x: geometry.size.width / 2, y: geometry.size.height * 1.05)
        }
        .background(.black)
    }
}

struct KompisWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: WidgetKind.kompis, provider: KompisProvider()) { _ in
            KompisView()
                .containerBackground(for: .widget) {
                    KompisBackground()
                }
                .widgetURL(URL(string: "salat://kompis/start"))
        }
        .configurationDisplayName("Kompis: Snakk nå")
        .description("Åpner Kompis og starter en samtale med en gang.")
        .supportedFamilies([.systemSmall, .accessoryCircular])
    }
}

/// «Snakk med Kompis» i kontrollsenteret, på låseskjermen eller på Handling-knappen.
struct KompisControl: ControlWidget {
    var body: some ControlWidgetConfiguration {
        StaticControlConfiguration(kind: WidgetKind.kompisControl) {
            ControlWidgetButton(action: StartKompisIntent()) {
                Label("Snakk med Kompis", systemImage: "waveform")
            }
        }
        .displayName("Snakk med Kompis")
        .description("Åpner Kompis og starter en samtale.")
    }
}
