import SwiftUI
import WidgetKit

/// Fargene til Rise: natt som går over i soloppgang.
enum RiseStyle {
    static let sunrise = LinearGradient(
        colors: [Color(hex: "#29235E"), Color(hex: "#6B3F86"), Color(hex: "#E0674A"), Color(hex: "#F7A35C")],
        startPoint: .top,
        endPoint: .bottom
    )

    static func time(_ date: Date) -> String {
        date.formatted(.dateTime.hour(.twoDigits(amPM: .omitted)).minute(.twoDigits).locale(Locale(identifier: "nb_NO")))
    }

    /// «I dag», «I morgen» eller ukedagen, f.eks. «Torsdag».
    static func day(_ date: Date, from now: Date) -> String {
        let calendar = Calendar.current
        if calendar.isDate(date, inSameDayAs: now) { return "I dag" }
        if let tomorrow = calendar.date(byAdding: .day, value: 1, to: now), calendar.isDate(date, inSameDayAs: tomorrow) {
            return "I morgen"
        }
        let weekday = date.formatted(.dateTime.weekday(.wide).locale(Locale(identifier: "nb_NO")))
        return weekday.prefix(1).uppercased() + weekday.dropFirst()
    }
}

struct NextAlarmEntry: TimelineEntry {
    let date: Date
    let alarm: RiseWidgetAlarm?
}

struct NextAlarmProvider: TimelineProvider {
    func placeholder(in context: Context) -> NextAlarmEntry {
        NextAlarmEntry(date: .now, alarm: RiseWidgetAlarm(title: "Trening", fireAtMillis: Int64((Date.now.timeIntervalSince1970 + 8 * 3600) * 1000)))
    }

    func getSnapshot(in context: Context, completion: @escaping (NextAlarmEntry) -> Void) {
        let next = upcoming(after: .now).first
        completion(next == nil && context.isPreview ? placeholder(in: context) : NextAlarmEntry(date: .now, alarm: next))
    }

    /// Én oppføring nå, og én hver gang en alarm har gått, så widgeten viser den neste.
    func getTimeline(in context: Context, completion: @escaping (Timeline<NextAlarmEntry>) -> Void) {
        let now = Date.now
        let alarms = upcoming(after: now)
        var entries = [NextAlarmEntry(date: now, alarm: alarms.first)]
        for (index, alarm) in alarms.prefix(10).enumerated() {
            entries.append(NextAlarmEntry(date: alarm.fireAt.addingTimeInterval(1), alarm: alarms.dropFirst(index + 1).first))
        }
        completion(Timeline(entries: entries, policy: .atEnd))
    }

    private func upcoming(after date: Date) -> [RiseWidgetAlarm] {
        (RiseWidgetData.load()?.alarms ?? []).filter { $0.fireAt > date }.sorted { $0.fireAtMillis < $1.fireAtMillis }
    }
}

struct NextAlarmView: View {
    let entry: NextAlarmEntry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        switch family {
        case .accessoryCircular: circular
        case .accessoryRectangular: rectangular
        case .accessoryInline: inline
        default: small
        }
    }

    private var small: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 6) {
                Image("RiseLogo")
                    .resizable()
                    .frame(width: 20, height: 20)
                    .clipShape(RoundedRectangle(cornerRadius: 5, style: .continuous))
                Text("NESTE ALARM")
                    .font(.system(size: 10, weight: .bold))
                    .tracking(0.8)
                    .opacity(0.75)
            }
            Spacer(minLength: 0)
            if let alarm = entry.alarm {
                Text(RiseStyle.day(alarm.fireAt, from: entry.date))
                    .font(.system(size: 13, weight: .medium))
                    .opacity(0.85)
                Text(RiseStyle.time(alarm.fireAt))
                    .font(.system(size: 42, weight: .bold, design: .rounded))
                    .minimumScaleFactor(0.7)
                    .lineLimit(1)
                Text(alarm.title)
                    .font(.system(size: 13, weight: .semibold))
                    .lineLimit(1)
                HStack(spacing: 4) {
                    Image(systemName: "hourglass")
                    Text(alarm.fireAt, style: .timer)
                }
                .font(.system(size: 11, weight: .medium).monospacedDigit())
                .opacity(0.8)
                .padding(.top, 2)
            } else {
                Text("Ingen alarmer")
                    .font(.system(size: 18, weight: .bold, design: .rounded))
                Text("Lag en i Rise")
                    .font(.system(size: 12))
                    .opacity(0.8)
            }
        }
        .foregroundStyle(.white)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
    }

    private var rectangular: some View {
        VStack(alignment: .leading, spacing: 0) {
            if let alarm = entry.alarm {
                HStack(spacing: 4) {
                    Image(systemName: "alarm.fill")
                    Text("\(RiseStyle.day(alarm.fireAt, from: entry.date)) · \(alarm.title)").lineLimit(1)
                }
                .font(.system(size: 12, weight: .semibold))
                .widgetAccentable()
                Text(RiseStyle.time(alarm.fireAt))
                    .font(.system(size: 24, weight: .bold, design: .rounded))
                HStack(spacing: 3) {
                    Text("om")
                    Text(alarm.fireAt, style: .timer)
                }
                .font(.system(size: 12, weight: .medium).monospacedDigit())
                .foregroundStyle(.secondary)
            } else {
                Label("Ingen alarmer", systemImage: "alarm")
                    .font(.system(size: 14, weight: .semibold))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var circular: some View {
        ZStack {
            AccessoryWidgetBackground()
            VStack(spacing: 1) {
                Image(systemName: "alarm.fill")
                    .font(.system(size: 11, weight: .semibold))
                    .widgetAccentable()
                Text(entry.alarm.map { RiseStyle.time($0.fireAt) } ?? "–")
                    .font(.system(size: 14, weight: .bold, design: .rounded))
                    .minimumScaleFactor(0.6)
            }
        }
    }

    @ViewBuilder
    private var inline: some View {
        if let alarm = entry.alarm {
            Text("\(Image(systemName: "alarm.fill")) \(RiseStyle.time(alarm.fireAt)) \(alarm.title)")
        } else {
            Text("\(Image(systemName: "alarm")) Ingen alarmer")
        }
    }
}

struct NextAlarmWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: WidgetKind.nextAlarm, provider: NextAlarmProvider()) { entry in
            NextAlarmView(entry: entry)
                .containerBackground(for: .widget) {
                    RiseStyle.sunrise
                }
                .widgetURL(URL(string: "salat://rise"))
        }
        .configurationDisplayName("Rise: Neste alarm")
        .description("Når neste alarm ringer, og hvor lenge det er til.")
        .supportedFamilies([.systemSmall, .accessoryCircular, .accessoryRectangular, .accessoryInline])
    }
}
