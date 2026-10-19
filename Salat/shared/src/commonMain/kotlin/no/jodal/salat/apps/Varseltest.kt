package no.jodal.salat.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import no.jodal.salat.MiniApp
import no.jodal.salat.NativeBridge
import no.jodal.salat.ui.SalatColors

val Varseltest = MiniApp(
    title = "Varseltest",
    emoji = "🔔",
    description = "Sjekk at varsler og lyd virker",
    color = SalatColors.Cheek,
    section = MiniApp.Section.Tools,
    content = { bridge -> VarseltestScreen(bridge) },
)

@Composable
private fun VarseltestScreen(bridge: NativeBridge) {
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
    }
}
