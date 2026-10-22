package no.jodal.salat.apps.brems.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import no.jodal.salat.NativeBridge
import no.jodal.salat.apps.brems.BremsStore
import no.jodal.salat.apps.brems.appName

/** Hvordan man lager automasjonene i Snarveier som forteller Brems når apper åpnes og lukkes. */
@Composable
fun GuideScreen(bridge: NativeBridge, store: BremsStore, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Header("Oppsett", onBack)
        Text(
            "iOS lar ikke apper se hva du bruker telefonen til. Derfor sier Snarveier fra til Brems når en app åpnes og lukkes. " +
                "Du lager to automasjoner, én gang.",
            style = MaterialTheme.typography.bodyLarge,
            color = BremsPalette.TextDim,
        )

        Status(store)

        Step("1", "Når apper åpnes")
        BremsCard {
            Steps(
                listOf(
                    "Åpne **Snarveier**, gå til **Automatisering** og trykk **+**.",
                    "Velg **App**. Trykk **Velg** og huk av appene du vil bremse, f.eks. Snapchat og YouTube.",
                    "Huk av **Er åpnet**, og ta bort **Er lukket**. Velg **Kjør umiddelbart** og trykk **Neste**.",
                    "Velg **Ny tom automatisering**, søk etter **Brems** og legg til **Brems: App åpnet**.",
                ),
            )
        }

        Step("2", "Når apper lukkes")
        BremsCard {
            Steps(
                listOf(
                    "Lag en ny automasjon med **App** og de samme appene.",
                    "Huk av **Er lukket**, og ta bort **Er åpnet**. Velg **Kjør umiddelbart**.",
                    "Legg til **Brems: App lukket**.",
                ),
            )
        }

        Step(null, "Tips")
        BremsCard {
            Text(
                "Snarveier sier ikke hvilken av appene som ble åpnet. Vil du se tiden per app, lag én åpnet-automasjon per app " +
                    "og skriv navnet i **App**-feltet i handlingen. Da kan Brems også sende deg rett tilbake til appen etter pausen.".bold(),
                style = MaterialTheme.typography.bodyMedium,
                color = BremsPalette.Text,
            )
        }

        Spacer(Modifier.height(16.dp))
        PrimaryButton("Åpne Snarveier", { bridge.openUrl("shortcuts://") })

        Step(null, "Logg")
        Log(store)
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Step(number: String?, title: String) {
    Spacer(Modifier.height(20.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
        if (number != null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp).clip(CircleShape).background(BremsPalette.Ice)) {
                Text(number, style = MaterialTheme.typography.labelLarge, color = BremsPalette.OnIce)
            }
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = BremsPalette.Text)
    }
}

/** Det siste Brems har hørt fra Snarveier, nyeste først. */
@Composable
private fun Log(store: BremsStore) {
    val entries = store.data.log.asReversed()
    BremsCard {
        if (entries.isEmpty()) {
            Text("Ingenting ennå.", style = MaterialTheme.typography.bodyMedium, color = BremsPalette.TextDim)
        }
        entries.forEachIndexed { index, entry ->
            if (index > 0) Spacer(Modifier.height(10.dp))
            Row {
                Text(clock(entry.atMillis, seconds = true), style = MaterialTheme.typography.labelMedium, color = BremsPalette.TextFaint, modifier = Modifier.width(72.dp))
                Text(entry.text, style = MaterialTheme.typography.bodySmall, color = BremsPalette.Text, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** «14:02», eller «14:02:09». */
private fun clock(millis: Long, seconds: Boolean = false): String {
    val time = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).time
    fun two(n: Int) = n.toString().padStart(2, '0')
    return "${two(time.hour)}:${two(time.minute)}" + if (seconds) ":${two(time.second)}" else ""
}

/** Om Brems har hørt fra Snarveier, så man ser at oppsettet virker. */
@Composable
private fun Status(store: BremsStore) {
    val last = store.data.opens.lastOrNull()
    val heard = last != null
    BremsCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(if (heard) BremsPalette.Ice else BremsPalette.Warn))
            Column {
                Text(
                    if (heard) "Brems hører fra Snarveier" else "Brems har ikke hørt noe ennå",
                    style = MaterialTheme.typography.titleMedium,
                    color = BremsPalette.Text,
                )
                Text(
                    if (last != null) {
                        "Sist: ${appName(last.app)} åpnet kl. ${clock(last.atMillis)}"
                    } else {
                        "Lag automasjonene, og åpne en av appene for å teste."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = BremsPalette.TextDim,
                )
            }
        }
    }
}

@Composable
private fun Steps(steps: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        steps.forEachIndexed { index, step ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("${index + 1}.", style = MaterialTheme.typography.titleSmall, color = BremsPalette.TextFaint, modifier = Modifier.width(18.dp))
                Text(step.bold(), style = MaterialTheme.typography.bodyMedium, color = BremsPalette.Text, modifier = Modifier.weight(1f).padding(top = 3.dp))
            }
        }
    }
}

/** Gjør **slik** tekst fet, som navnene på knappene i Snarveier. */
private fun String.bold() = buildAnnotatedString {
    split("**").forEachIndexed { index, part ->
        if (index % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = BremsPalette.Ice)) { append(part) } else append(part)
    }
}
