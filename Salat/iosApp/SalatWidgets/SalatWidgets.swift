import SwiftUI
import WidgetKit

/// Alle widgetene i Salat. De ligger i én utvidelse, så de bare bruker én App-ID i SideStore.
@main
struct SalatWidgets: WidgetBundle {
    var body: some Widget {
        SoundboardWidget()
        FavoriteSoundWidget()
        NextAlarmWidget()
        KompisWidget()
        KompisControl()
    }
}

extension Color {
    /// En farge fra `#RRGGBB`.
    init(hex: String) {
        let value = UInt32(hex.trimmingCharacters(in: CharacterSet(charactersIn: "#")), radix: 16) ?? 0
        self.init(
            red: Double((value >> 16) & 0xFF) / 255,
            green: Double((value >> 8) & 0xFF) / 255,
            blue: Double(value & 0xFF) / 255
        )
    }
}
