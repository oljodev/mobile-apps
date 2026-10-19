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
import no.jodal.salat.MiniApp
import no.jodal.salat.NativeBridge
import no.jodal.salat.ui.SalatColors

val Varseltest = MiniApp(
    title = "Varseltest",
    emoji = "🔔",
    description = "Sjekk at varsler, alarmer og lyd virker",
    color = SalatColors.Cheek,
    section = MiniApp.Section.Tools,
    content = { bridge -> VarseltestScreen(bridge) },
)

@Composable
private fun VarseltestScreen(bridge: NativeBridge) {
    var alarmResult by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Trykk på en knapp og lås telefonen. Varselet skal komme med lyd.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = { bridge.sendTestNotification(delaySeconds = 5) }) {
            Text("Varsel om 5 sekunder")
        }
        Button(onClick = { bridge.sendTestNotification(delaySeconds = 60) }) {
            Text("Varsel om 1 minutt")
        }

        Text(
            "Alarmer ringer gjennom lydløs og Fokus. Sett telefonen på lydløs og lås den for å teste.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
        Button(onClick = {
            alarmResult = "Planlegger …"
            bridge.scheduleTestAlarm(delaySeconds = 60, snoozeMinutes = 0) { error ->
                alarmResult = error ?: "Alarmen ringer om 1 minutt."
            }
        }) {
            Text("Alarm om 1 minutt")
        }
        Button(onClick = {
            alarmResult = "Planlegger …"
            bridge.scheduleTestAlarm(delaySeconds = 60, snoozeMinutes = 5) { error ->
                alarmResult = error ?: "Alarmen ringer om 1 minutt. Trykk Slumre, så skal den ringe igjen etter 5 minutter."
            }
        }) {
            Text("Alarm om 1 minutt, med slumring")
        }
        alarmResult?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}
