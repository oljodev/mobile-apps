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
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import no.jodal.salat.MiniApp
import no.jodal.salat.NativeBridge

val Fornyelse = MiniApp(
    title = "Fornyelse",
    emoji = "⏳",
    description = "Hvor lenge til appen må fornyes i SideStore",
    content = { bridge -> FornyelseScreen(bridge) },
)

@Composable
private fun FornyelseScreen(bridge: NativeBridge) {
    val expiresAt = remember { bridge.signingExpiresAtMillis() }
    var reminderScheduled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (expiresAt == null) {
            Text(
                "Fant ikke utløpsdatoen. Det skjer hvis appen ikke er installert gjennom SideStore.",
                style = MaterialTheme.typography.bodyLarge,
            )
            return@Column
        }

        val remaining = (expiresAt - Clock.System.now().toEpochMilliseconds()).milliseconds
        val (text, color) = when {
            remaining.isNegative() -> "Signeringen har gått ut" to MaterialTheme.colorScheme.error
            remaining.inWholeDays < 2 -> "${formatRemaining(remaining.inWholeHours)} igjen" to MaterialTheme.colorScheme.error
            else -> "${formatRemaining(remaining.inWholeHours)} igjen" to MaterialTheme.colorScheme.primary
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

private fun formatRemaining(hours: Long): String {
    val days = hours / 24
    val restHours = hours % 24
    val dayText = if (days == 1L) "1 dag" else "$days dager"
    val hourText = if (restHours == 1L) "1 time" else "$restHours timer"
    return if (days > 0) "$dayText og $hourText" else hourText
}
