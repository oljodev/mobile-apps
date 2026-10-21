package no.jodal.salat.apps.ekko.ui

import no.jodal.salat.Haptics
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import no.jodal.salat.apps.ekko.Clip
import no.jodal.salat.apps.ekko.formatLength

/** [levels] skalert til [count] streker: den høyeste verdien i hver bit. */
fun resample(levels: List<Float>, count: Int): List<Float> {
    if (count <= 0) return emptyList()
    if (levels.isEmpty()) return List(count) { i -> 0.3f + 0.25f * sin(i * 1.7f).let { it * it } }
    return List(count) { i ->
        val from = (i.toLong() * levels.size / count).toInt().coerceIn(0, levels.lastIndex)
        val to = ((i + 1).toLong() * levels.size / count).toInt().coerceIn(from + 1, levels.size)
        levels.subList(from, to).max()
    }
}

/**
 * En lydbølge som streker. Strekene til venstre for [progress] får fargen [playedColor], så den
 * kan brukes som fremdriftslinje.
 */
@Composable
fun Waveform(
    levels: List<Float>,
    bars: Int,
    color: Color,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    playedColor: Color = color,
    gap: Float = 0.6f,
    minHeight: Float = 0.1f,
) {
    val sampled = resample(levels, bars)
    Canvas(modifier) {
        val barWidth = size.width / (bars + (bars - 1) * gap)
        val step = barWidth * (1 + gap)
        sampled.forEachIndexed { i, level ->
            val h = size.height * level.coerceIn(minHeight, 1f)
            val x = i * step
            val played = progress != null && (x + barWidth / 2) / size.width <= progress
            drawRoundRect(
                color = if (played) playedColor else color,
                topLeft = Offset(x, (size.height - h) / 2),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2),
            )
        }
    }
}

/** Omslaget til en lyd: to farger og lydbølgen fra utsnittet. */
@Composable
fun ClipCover(clip: Clip, size: Dp, modifier: Modifier = Modifier, corner: Dp = size * 0.2f, bars: Int = 9) {
    val (from, to) = EkkoPalette.cover(clip.id)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(Brush.linearGradient(listOf(from, to))),
    ) {
        Waveform(
            levels = clip.trimmedWaveform(),
            bars = bars,
            color = EkkoPalette.Ink.copy(alpha = 0.55f),
            gap = 0.75f,
            minHeight = 0.12f,
            modifier = Modifier.fillMaxSize().padding(horizontal = size * 0.2f, vertical = size * 0.26f),
        )
    }
}

/** Omslaget til en spilleliste: de fire første lydene, eller én farge hvis den er tom. */
@Composable
fun PlaylistCover(id: String, clips: List<Clip>, size: Dp, corner: Dp = size * 0.16f) {
    Box(Modifier.size(size).clip(RoundedCornerShape(corner)).background(EkkoPalette.SurfaceHigh)) {
        when {
            clips.size >= 4 -> Column {
                for (row in 0..1) Row {
                    for (col in 0..1) ClipCover(clips[row * 2 + col], size / 2, corner = 0.dp, bars = 5)
                }
            }
            clips.isNotEmpty() -> ClipCover(clips.first(), size, corner = 0.dp)
            else -> {
                val (from, to) = EkkoPalette.cover(id)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(from.copy(alpha = 0.35f), to.copy(alpha = 0.2f)))),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null, tint = from, modifier = Modifier.size(size * 0.4f))
                }
            }
        }
    }
}

/** Tre små streker som hopper, for lyden som spilles nå. */
@Composable
fun PlayingBars(playing: Boolean, color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val time by transition.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(900, easing = LinearEasing)))
    Canvas(modifier.size(16.dp)) {
        val barWidth = size.width / 4.2f
        for (i in 0..2) {
            val level = if (playing) 0.35f + 0.65f * ((sin(time * (1 + i * 0.35f) + i * 2.1f) + 1) / 2) else 0.35f
            val h = size.height * level
            drawRoundRect(color, Offset(i * barWidth * 1.6f, size.height - h), Size(barWidth, h), CornerRadius(barWidth / 2))
        }
    }
}

/** En lyd i en liste. Trykk for å spille, hold inne eller trykk på «…» for mer. */
@Composable
fun ClipRow(clip: Clip, isCurrent: Boolean, isPlaying: Boolean, onClick: () -> Unit, onMore: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .pressable(onClick, pressedScale = 0.97f, onLongClick = onMore)
            .padding(horizontal = 8.dp, vertical = 7.dp),
    ) {
        ClipCover(clip, 52.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                clip.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) EkkoPalette.Lime else EkkoPalette.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isCurrent) PlayingBars(isPlaying, EkkoPalette.Lime, Modifier.size(12.dp))
                Text(
                    listOfNotNull(clip.artist.ifBlank { null }, formatLength(clip.lengthSeconds)).joinToString("  ·  "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = EkkoPalette.TextDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        RoundIconButton(Icons.Rounded.MoreHoriz, "Mer", onMore, size = 36.dp, background = Color.Transparent, tint = EkkoPalette.TextDim)
    }
}

/** En rund knapp med et ikon. */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    size: Dp = 40.dp,
    background: Color = EkkoPalette.Surface,
    tint: Color = EkkoPalette.Text,
    iconSize: Dp = size * 0.5f,
    enabled: Boolean = true,
    haptic: () -> Unit = Haptics::tap,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(CircleShape)
            .background(background)
            .pressable(onClick, enabled = enabled, pressedScale = 0.9f, haptic = haptic),
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Den store lime-knappen. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    PillButton(text, onClick, modifier, icon, enabled, EkkoPalette.Lime, EkkoPalette.OnLime)
}

/** En dempet knapp, for det som ikke er hovedvalget. */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    PillButton(text, onClick, modifier, icon, enabled, EkkoPalette.SurfaceHigh, EkkoPalette.Text)
}

@Composable
private fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    icon: ImageVector?,
    enabled: Boolean,
    background: Color,
    content: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        modifier = modifier
            .height(52.dp)
            .clip(CircleShape)
            .background(if (enabled) background else EkkoPalette.Surface)
            .pressable(onClick, enabled = enabled, pressedScale = 0.97f)
            .padding(horizontal = 22.dp),
    ) {
        val color = if (enabled) content else EkkoPalette.TextDim
        if (icon != null) Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

/** Et tekstfelt i Ekko sin stil. */
@Composable
fun EkkoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    capitalize: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    onDone: () -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {
        if (value.isNotEmpty()) {
            RoundIconButton(Icons.Rounded.Close, "Tøm", { onValueChange("") }, size = 24.dp, background = EkkoPalette.Line, iconSize = 14.dp)
        }
    },
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(EkkoPalette.Surface)
            .padding(start = 16.dp, end = 12.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = EkkoPalette.Text),
            cursorBrush = SolidColor(EkkoPalette.Lime),
            keyboardOptions = KeyboardOptions(
                capitalization = if (capitalize) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
                imeAction = imeAction,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }, onGo = { onDone() }),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = EkkoPalette.TextDim, maxLines = 1)
                    }
                    inner()
                }
            },
        )
        trailing()
    }
}

/** En liten overskrift over en del av skjermen. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = EkkoPalette.TextDim, modifier = modifier)
}

/** En rad i et ark med valg. */
@Composable
fun SheetAction(icon: ImageVector, text: String, onClick: () -> Unit, color: Color = EkkoPalette.Text) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .pressable(onClick, pressedScale = 0.98f)
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

/** En tynn skillestrek. */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(EkkoPalette.Line))
}

/** Søkefeltet øverst i Ekko. */
@Composable
fun SearchField(query: String, onQuery: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(EkkoPalette.Surface)
            .padding(start = 12.dp, end = 8.dp),
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = EkkoPalette.TextDim, modifier = Modifier.size(20.dp))
        BasicTextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = EkkoPalette.Text),
            cursorBrush = SolidColor(EkkoPalette.Lime),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) {
                        Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = EkkoPalette.TextDim, maxLines = 1)
                    }
                    inner()
                }
            },
        )
        if (query.isNotEmpty()) {
            RoundIconButton(Icons.Rounded.Close, "Tøm søket", { onQuery("") }, size = 22.dp, background = EkkoPalette.Line, iconSize = 13.dp)
        }
    }
}
