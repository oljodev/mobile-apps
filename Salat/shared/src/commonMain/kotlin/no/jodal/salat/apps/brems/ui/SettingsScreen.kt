package no.jodal.salat.apps.brems.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import no.jodal.salat.Haptics
import no.jodal.salat.apps.brems.BremsStore
import no.jodal.salat.apps.brems.DefaultQuotes
import no.jodal.salat.apps.brems.DefaultReasons
import no.jodal.salat.ui.pressable

private val OpenPauseSteps = listOf(0, 5, 10, 15, 20, 30, 45, 60)
private val ReminderSteps = listOf(0, 1, 2, 3, 5, 7, 10, 15, 20, 30, 45, 60)
private val LimitSteps = listOf(1, 2, 3, 5, 10, 15, 20, 30, 45, 60, 90, 120)
private val LimitPauseSteps = listOf(15, 30, 45, 60, 90, 120, 180, 300, 600)
private val NagSteps = listOf(0, 1, 2, 3, 5, 10)

@Composable
fun SettingsScreen(store: BremsStore, onBack: () -> Unit) {
    val settings = store.data.settings
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Header("Innstillinger", onBack)

        BremsCard(padding = 0.dp) {
            Box(Modifier.padding(horizontal = 20.dp)) {
                ToggleRow("Brems er på", "Skru av for å slippe pauser og varsler en stund", settings.enabled) { on ->
                    store.updateSettings { it.copy(enabled = on) }
                }
            }
        }

        SectionLabel("Pauser og grenser", Modifier.padding(top = 12.dp))
        BremsCard(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Stepper("Pause når du åpner", "Hver gang du åpner en app", settings.openPauseSeconds, OpenPauseSteps, ::seconds) { v ->
                    store.updateSettings { it.copy(openPauseSeconds = v) }
                }
                Divider()
                Stepper("Varsel etter", "Et varsel mens du er i appen", settings.reminderMinutes, ReminderSteps, ::minutes) { v ->
                    store.updateSettings { it.copy(reminderMinutes = v) }
                }
                Divider()
                Stepper("Grense per økt", "Så lenge du kan være i en app i strekk", settings.limitMinutes, LimitSteps, ::minutes) { v ->
                    store.updateSettings { it.copy(limitMinutes = v) }
                }
                Divider()
                Stepper("Pause etter grensen", "Tiden du er borte, teller med", settings.limitPauseSeconds, LimitPauseSteps, ::seconds) { v ->
                    store.updateSettings { it.copy(limitPauseSeconds = v) }
                }
                Divider()
                Stepper("Mas etter grensen", "Nytt varsel så ofte du er over", settings.nagMinutes, NagSteps, { if (it == 0) "Av" else "$it min" }) { v ->
                    store.updateSettings { it.copy(nagMinutes = v) }
                }
            }
        }

        SectionLabel("Pauseskjermen", Modifier.padding(top = 12.dp))
        BremsCard(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                ToggleRow("Pust", "En sirkel som vokser og krymper i takt med pusten", settings.breathing) { on ->
                    store.updateSettings { it.copy(breathing = on) }
                }
                Divider()
                ToggleRow("Dagens bruk", "Hvor lenge og hvor ofte i dag", settings.showUsage) { on ->
                    store.updateSettings { it.copy(showUsage = on) }
                }
                Divider()
                ToggleRow("Spør hvorfor", "Velg en grunn mens du venter", settings.askWhy) { on ->
                    store.updateSettings { it.copy(askWhy = on) }
                }
                Divider()
                ToggleRow("Påminnelse", "En kort tekst, tilfeldig valgt", settings.showQuote) { on ->
                    store.updateSettings { it.copy(showQuote = on) }
                }
            }
        }

        if (settings.askWhy) {
            SectionLabel("Grunner", Modifier.padding(top = 12.dp))
            TextList(
                items = settings.reasons,
                placeholder = "Ny grunn",
                onChange = { list -> store.updateSettings { it.copy(reasons = list) } },
                onReset = if (settings.reasons != DefaultReasons) ({ store.updateSettings { it.copy(reasons = DefaultReasons) } }) else null,
            )
        }

        if (settings.showQuote) {
            SectionLabel("Påminnelser", Modifier.padding(top = 12.dp))
            TextList(
                items = settings.quotes,
                placeholder = "Skriv din egen",
                onChange = { list -> store.updateSettings { it.copy(quotes = list) } },
                onReset = if (settings.quotes != DefaultQuotes) ({ store.updateSettings { it.copy(quotes = DefaultQuotes) } }) else null,
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

/** Tilbakeknapp og tittel øverst på en underside. */
@Composable
fun Header(title: String, onBack: () -> Unit) {
    Column(Modifier.padding(top = 4.dp, bottom = 8.dp)) {
        IconCircle(Icons.AutoMirrored.Rounded.ArrowBack, "Tilbake", onBack)
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = BremsPalette.Text)
    }
}

@Composable
private fun Stepper(title: String, subtitle: String, value: Int, steps: List<Int>, format: (Int) -> String, onChange: (Int) -> Unit) {
    val index = steps.indexOfFirst { it >= value }.let { if (it < 0) steps.lastIndex else it }
    StepperRow(
        title = title,
        subtitle = subtitle,
        value = format(value),
        onMinus = { onChange(steps[(index - 1).coerceAtLeast(0)]) },
        onPlus = { onChange(steps[(if (steps[index] == value) index + 1 else index).coerceAtMost(steps.lastIndex)]) },
        canMinus = value > steps.first(),
        canPlus = value < steps.last(),
    )
}

private fun seconds(value: Int): String = when {
    value == 0 -> "Av"
    value < 60 -> "$value s"
    value % 60 == 0 -> "${value / 60} min"
    else -> "${value / 60}:${(value % 60).toString().padStart(2, '0')}"
}

private fun minutes(value: Int): String = if (value == 0) "Av" else "$value min"

/** En liste med tekster man kan slette fra og legge til i. */
@Composable
private fun TextList(items: List<String>, placeholder: String, onChange: (List<String>) -> Unit, onReset: (() -> Unit)?) {
    var draft by remember { mutableStateOf("") }
    fun add() {
        val text = draft.trim()
        if (text.isNotEmpty() && text !in items) {
            onChange(items + text)
            Haptics.success()
        }
        draft = ""
    }
    BremsCard(padding = 0.dp) {
        items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp)) {
                Text(item, style = MaterialTheme.typography.bodyLarge, color = BremsPalette.Text, modifier = Modifier.weight(1f).padding(vertical = 14.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(40.dp).clip(CircleShape).pressable({ onChange(items - item) }, pressedScale = 0.85f),
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Fjern", tint = BremsPalette.TextDim, modifier = Modifier.size(18.dp))
                }
            }
            Divider()
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp)) {
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = BremsPalette.Text),
                cursorBrush = SolidColor(BremsPalette.Ice),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() }),
                modifier = Modifier.weight(1f).padding(vertical = 14.dp),
                decorationBox = { inner ->
                    Box {
                        if (draft.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = BremsPalette.TextFaint)
                        inner()
                    }
                },
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (draft.isBlank()) BremsPalette.SurfaceHigh else BremsPalette.Ice)
                    .pressable(::add, enabled = draft.isNotBlank(), pressedScale = 0.85f),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Legg til", tint = if (draft.isBlank()) BremsPalette.TextDim else BremsPalette.OnIce, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.size(4.dp))
        }
    }
    if (onReset != null) {
        Text(
            "Tilbakestill",
            style = MaterialTheme.typography.labelLarge,
            color = BremsPalette.Ice,
            modifier = Modifier.padding(start = 4.dp).pressable(onReset),
        )
    }
}
