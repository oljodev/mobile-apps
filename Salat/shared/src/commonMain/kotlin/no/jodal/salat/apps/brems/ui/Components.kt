package no.jodal.salat.apps.brems.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

/** Et kort med en tynn kant, som alt i Brems ligger i. */
@Composable
fun BremsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(onClick, pressedScale = 0.98f) else Modifier)
            .clip(shape)
            .background(BremsPalette.Surface)
            .border(1.dp, BremsPalette.Line, shape)
            .padding(padding),
        content = content,
    )
}

/** En liten overskrift over en del av skjermen, i store bokstaver. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = BremsPalette.TextDim,
        modifier = modifier.padding(start = 4.dp),
    )
}

@Composable
fun IconCircle(icon: ImageVector, description: String, onClick: () -> Unit, size: Dp = 40.dp, tint: Color = BremsPalette.Text) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(BremsPalette.Surface)
            .border(1.dp, BremsPalette.Line, CircleShape)
            .pressable(onClick, pressedScale = 0.9f),
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** Den isblå hovedknappen. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(RoundedCornerShape(18.dp))
            .background(BremsPalette.Ice)
            .pressable(onClick, enabled = enabled, pressedScale = 0.97f),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = BremsPalette.OnIce)
    }
}

/** En dempet knapp, for det som ikke er hovedvalget. */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(BremsPalette.Surface)
            .border(1.dp, BremsPalette.Line, shape)
            .pressable(onClick, pressedScale = 0.97f),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = BremsPalette.Text)
    }
}

/** En brikke man kan velge, f.eks. en grunn til å åpne appen. */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    val background by animateColorAsState(if (selected) BremsPalette.Ice else BremsPalette.Surface)
    val content by animateColorAsState(if (selected) BremsPalette.OnIce else BremsPalette.Text)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(background)
            .border(1.dp, if (selected) BremsPalette.Ice else BremsPalette.Line, shape)
            .pressable(onClick, pressedScale = 0.94f, haptic = Haptics::select)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = content)
    }
}

/** En rad med en tittel og et tall man kan skru opp og ned. */
@Composable
fun StepperRow(
    title: String,
    subtitle: String?,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    canMinus: Boolean = true,
    canPlus: Boolean = true,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = BremsPalette.Text)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = BremsPalette.TextDim)
            }
        }
        StepButton(Icons.Rounded.Remove, "Mindre", onMinus, canMinus)
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = BremsPalette.Text,
            modifier = Modifier.width(76.dp).padding(horizontal = 4.dp),
            textAlign = TextAlign.Center,
        )
        StepButton(Icons.Rounded.Add, "Mer", onPlus, canPlus)
    }
}

@Composable
private fun StepButton(icon: ImageVector, description: String, onClick: () -> Unit, enabled: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .alpha(if (enabled) 1f else 0.3f)
            .clip(CircleShape)
            .background(BremsPalette.SurfaceHigh)
            .pressable(onClick, enabled = enabled, pressedScale = 0.88f, haptic = Haptics::select),
    ) {
        Icon(icon, contentDescription = description, tint = BremsPalette.Text, modifier = Modifier.size(18.dp))
    }
}

/** En rad med en bryter. */
@Composable
fun ToggleRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().pressable({ onChange(!checked) }, pressedScale = 0.99f, haptic = Haptics::select).padding(vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = BremsPalette.Text)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = BremsPalette.TextDim)
            }
        }
        Switch(checked)
    }
}

@Composable
private fun Switch(checked: Boolean) {
    val track by animateColorAsState(if (checked) BremsPalette.Ice else BremsPalette.SurfaceHigh)
    val knob by animateDpAsState(if (checked) 22.dp else 2.dp, spring(dampingRatio = 0.7f, stiffness = 600f))
    Box(Modifier.width(50.dp).height(30.dp).clip(CircleShape).background(track)) {
        Box(
            Modifier
                .offset(x = knob, y = 2.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(if (checked) BremsPalette.OnIce else BremsPalette.TextDim),
        )
    }
}

/** En tynn strek mellom radene i et kort. */
@Composable
fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(BremsPalette.Line))
}
