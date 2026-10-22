package no.jodal.salat.apps.brems.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import no.jodal.salat.Haptics
import no.jodal.salat.resources.Res
import no.jodal.salat.resources.brems_logo
import no.jodal.salat.ui.pressable
import org.jetbrains.compose.resources.painterResource

/** Logoen i en avrundet firkant. */
@Composable
fun BremsLogo(size: Dp) {
    Image(
        painterResource(Res.drawable.brems_logo),
        contentDescription = null,
        modifier = Modifier.size(size).clip(RoundedCornerShape(size * 0.24f)),
    )
}

/** En flate uten kant. Brems bruker få av dem, og heller mye luft. */
@Composable
fun BremsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(onClick, pressedScale = 0.98f) else Modifier)
            .clip(RoundedCornerShape(28.dp))
            .background(BremsPalette.Surface)
            .padding(padding),
        content = content,
    )
}

/** Overskriften over en del av skjermen, med en valgfri handling til høyre. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = BremsPalette.Text, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(action, style = MaterialTheme.typography.labelLarge, color = BremsPalette.Ice, modifier = Modifier.offset(x = 8.dp).pressable(onAction).padding(8.dp))
        }
    }
}

/** En liten, dempet tekst over noe, f.eks. «I dag». */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = BremsPalette.TextDim) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color, modifier = modifier)
}

/** Et ikon uten bakgrunn, med god plass å trykke på. */
@Composable
fun PlainIconButton(icon: ImageVector, description: String, onClick: () -> Unit, tint: Color = BremsPalette.TextDim) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(44.dp).clip(CircleShape).pressable(onClick, pressedScale = 0.85f),
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(24.dp))
    }
}

/** Den isblå hovedknappen. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(CircleShape)
            .background(BremsPalette.Ice)
            .pressable(onClick, enabled = enabled, pressedScale = 0.97f),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = BremsPalette.OnIce)
    }
}

/** En knapp som bare er tekst, for det man helst ikke skal velge. */
@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth().height(52.dp).clip(CircleShape).pressable(onClick, pressedScale = 0.97f),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = BremsPalette.TextDim)
    }
}

/** En brikke man kan velge, f.eks. en grunn til å åpne appen. */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(if (selected) BremsPalette.Ice else BremsPalette.SurfaceHigh)
    val content by animateColorAsState(if (selected) BremsPalette.OnIce else BremsPalette.Text)
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .pressable(onClick, pressedScale = 0.94f, haptic = Haptics::select)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = content)
    }
}

/** En rad med et tall man kan skru opp og ned, i en liten kapsel til høyre. */
@Composable
fun StepperRow(
    title: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    canMinus: Boolean = true,
    canPlus: Boolean = true,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(64.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = BremsPalette.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clip(CircleShape).background(BremsPalette.SurfaceHigh),
        ) {
            StepButton(Icons.Rounded.Remove, "Mindre", onMinus, canMinus)
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                color = BremsPalette.Text,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 56.dp),
            )
            StepButton(Icons.Rounded.Add, "Mer", onPlus, canPlus)
        }
    }
}

@Composable
private fun StepButton(icon: ImageVector, description: String, onClick: () -> Unit, enabled: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .alpha(if (enabled) 1f else 0.25f)
            .clip(CircleShape)
            .pressable(onClick, enabled = enabled, pressedScale = 0.85f, haptic = Haptics::select),
    ) {
        Icon(icon, contentDescription = description, tint = BremsPalette.Ice, modifier = Modifier.size(18.dp))
    }
}

/** En rad med en bryter. */
@Composable
fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().height(64.dp).pressable({ onChange(!checked) }, pressedScale = 0.99f, haptic = Haptics::select),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = BremsPalette.Text, modifier = Modifier.weight(1f))
        Switch(checked)
    }
}

@Composable
private fun Switch(checked: Boolean) {
    val track by animateColorAsState(if (checked) BremsPalette.Ice else BremsPalette.SurfaceHigh)
    val knob by animateDpAsState(if (checked) 20.dp else 2.dp, spring(dampingRatio = 0.7f, stiffness = 600f))
    Box(Modifier.width(48.dp).height(30.dp).clip(CircleShape).background(track)) {
        Box(
            Modifier
                .offset(x = knob, y = 2.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(if (checked) BremsPalette.OnIce else BremsPalette.TextDim),
        )
    }
}

/** En hårfin strek mellom radene i en flate. */
@Composable
fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(BremsPalette.Line))
}

/** Dempet tekst under en flate, som forklarer den. */
@Composable
fun Footnote(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = BremsPalette.TextDim, modifier = modifier.padding(horizontal = 20.dp))
}
