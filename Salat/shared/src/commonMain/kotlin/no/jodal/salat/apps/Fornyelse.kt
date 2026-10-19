package no.jodal.salat.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HourglassTop
import no.jodal.salat.MiniApp
import no.jodal.salat.MiniAppIcon
import no.jodal.salat.NativeBridge
import no.jodal.salat.ui.SalatColors
import no.jodal.salat.ui.formatTimeLeft
import no.jodal.salat.ui.signingTimeLeft

val Fornyelse = MiniApp(
    title = "Fornyelse",
    description = "Når appen må fornyes i SideStore",
    color = SalatColors.Leaf,
    section = MiniApp.Section.Tools,
    icon = MiniAppIcon.Symbol(Icons.Rounded.HourglassTop),
    content = { FornyelseScreen(it.bridge) },
)

@Composable
private fun FornyelseScreen(bridge: NativeBridge) {
    val remaining = remember { bridge.signingTimeLeft() }
    var reminderScheduled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (remaining == null) {
            Text(
                "Fant ikke utløpsdatoen. Det skjer hvis appen ikke er installert gjennom SideStore.",
                style = MaterialTheme.typography.bodyLarge,
            )
            return@Column
        }

        val (text, color) = when {
            remaining.isNegative() -> "Signeringen har gått ut" to MaterialTheme.colorScheme.error
            remaining.inWholeDays < 2 -> "${formatTimeLeft(remaining)} igjen" to MaterialTheme.colorScheme.error
            else -> "${formatTimeLeft(remaining)} igjen" to MaterialTheme.colorScheme.primary
        }
        Text(text, style = MaterialTheme.typography.headlineMedium, color = color)
        Text(
            "Salat varsler deg dagen før den går ut. Åpne SideStore og trykk «Refresh All» for å fornye.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = {
            bridge.scheduleExpiryReminder()
            reminderScheduled = true
        }) {
            Text("Planlegg påminnelsen på nytt")
        }
        if (reminderScheduled) {
            Text("Påminnelsen er planlagt.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
