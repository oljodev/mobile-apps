package no.jodal.salat.apps.brems.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
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
import androidx.compose.ui.draw.shadow
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

/** Logoen i en avrundet firkant, som et app-ikon. */
@Composable
fun BremsLogo(size: Dp) {
    Image(
        painterResource(Res.drawable.brems_logo),
        contentDescription = null,
        modifier = Modifier.size(size).clip(RoundedCornerShape(size * 0.23f)),
    )
}

/** Et hvitt kort med en myk skygge, eller et mørkt kort uten i mørk modus. */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    val shadow = if (isSystemInDarkTheme()) Modifier else Modifier.shadow(10.dp, shape, ambientColor = Color(0x10000000), spotColor = Color(0x14000000))
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(onClick, pressedScale = 0.98f) else Modifier)
            .then(shadow)
            .clip(shape)
            .background(palette.card)
            .padding(padding),
        content = content,
    )
}

/** Overskriften over et kort, liten og dempet, som i Innstillinger i iOS. */
@Composable
fun GroupLabel(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    Row(verticalAlignment = Alignment.Bottom, modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, bottom = 8.dp)) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = palette.textDim, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(
                action,
                style = MaterialTheme.typography.titleSmall,
                color = palette.accent,
                modifier = Modifier.offset(y = 6.dp).pressable(onAction).padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

/** Forklarende tekst under et kort. */
@Composable
fun Footnote(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = palette.textDim, modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp))
}

/** Et hvitt symbol i en farget, avrundet rute, som foran radene i Innstillinger. */
@Composable
fun IconTile(icon: ImageVector, tint: Color, size: Dp = 30.dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size).clip(RoundedCornerShape(size * 0.27f)).background(tint),
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.6f))
    }
}

/** En rad i et kort: ikon, tittel, og noe til høyre. */
@Composable
fun ListRow(
    title: String,
    icon: ImageVector? = null,
    iconTint: Color = palette.accent,
    subtitle: String? = null,
    value: String? = null,
    chevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .then(if (onClick != null) Modifier.pressable(onClick, pressedScale = 0.99f) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        if (icon != null) IconTile(icon, iconTint)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.textDim, maxLines = 2)
            }
        }
        if (value != null) Text(value, style = MaterialTheme.typography.bodyLarge, color = palette.textDim)
        trailing?.invoke()
        if (chevron) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = null, tint = palette.textFaint, modifier = Modifier.size(13.dp))
        }
    }
}

/** En strek mellom radene, rykket inn forbi ikonet. */
@Composable
fun RowDivider(inset: Dp = 60.dp) {
    Box(Modifier.padding(start = inset).fillMaxWidth().height(0.5.dp).background(palette.line))
}

/** Et tall man kan skru opp og ned. */
@Composable
fun Stepper(value: String, onMinus: () -> Unit, onPlus: () -> Unit, canMinus: Boolean, canPlus: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(palette.cardHigh),
    ) {
        StepButton(Icons.Rounded.Remove, "Mindre", onMinus, canMinus)
        if (value.isNotEmpty()) {
            Text(
                value,
                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                color = palette.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 52.dp),
            )
        } else {
            Box(Modifier.width(1.dp).height(18.dp).background(palette.line))
        }
        StepButton(Icons.Rounded.Add, "Mer", onPlus, canPlus)
    }
}

@Composable
private fun StepButton(icon: ImageVector, description: String, onClick: () -> Unit, enabled: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 38.dp, height = 34.dp)
            .alpha(if (enabled) 1f else 0.3f)
            .pressable(onClick, enabled = enabled, pressedScale = 0.85f, haptic = Haptics::select),
    ) {
        Icon(icon, contentDescription = description, tint = palette.text, modifier = Modifier.size(16.dp))
    }
}

/** En bryter som i iOS. */
@Composable
fun Switch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val track by animateColorAsState(if (checked) palette.accent else palette.cardHigh)
    val knob by animateDpAsState(if (checked) 22.dp else 2.dp, spring(dampingRatio = 0.7f, stiffness = 600f))
    Box(
        Modifier
            .width(51.dp)
            .height(31.dp)
            .clip(CircleShape)
            .background(track)
            .pressable({ onChange(!checked) }, pressedScale = 0.95f, haptic = Haptics::select),
    ) {
        Box(
            Modifier
                .offset(x = knob, y = 2.dp)
                .size(27.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

/** Den fylte knappen i aksentfargen. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(16.dp))
            .background(palette.accent)
            .pressable(onClick, enabled = enabled, pressedScale = 0.97f),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = palette.onAccent)
    }
}

/** Tilbakeknapp øverst, med noe valgfritt til høyre. */
@Composable
fun NavBar(backLabel: String, onBack: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(44.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.offset(x = (-6).dp).clip(RoundedCornerShape(10.dp)).pressable(onBack).padding(horizontal = 6.dp, vertical = 8.dp),
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, contentDescription = "Tilbake", tint = palette.accent, modifier = Modifier.size(18.dp))
            Text(backLabel, style = MaterialTheme.typography.bodyLarge, color = palette.accent)
        }
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** Den store tittelen øverst på en side. */
@Composable
fun LargeTitle(title: String, subtitle: String? = null) {
    Column(Modifier.padding(top = 4.dp, bottom = 20.dp)) {
        if (subtitle != null) {
            Text(subtitle.uppercase(), style = MaterialTheme.typography.labelSmall, color = palette.textDim)
            Spacer(Modifier.height(4.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineLarge, color = palette.text)
    }
}

/** En rund knapp med bare et ikon, f.eks. innstillinger. */
@Composable
fun CircleIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(36.dp).clip(CircleShape).background(palette.cardHigh).pressable(onClick, pressedScale = 0.88f),
    ) {
        Icon(icon, contentDescription = description, tint = palette.text, modifier = Modifier.size(20.dp))
    }
}
