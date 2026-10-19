package no.jodal.salat.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import no.jodal.salat.MiniApp
import no.jodal.salat.NativeBridge
import no.jodal.salat.apps.Fornyelse
import no.jodal.salat.apps.rise.RiseStore
import no.jodal.salat.apps.rise.ui.AwakeCard
import no.jodal.salat.miniApps
import no.jodal.salat.resources.Res
import no.jodal.salat.resources.salat_logo
import org.jetbrains.compose.resources.painterResource

@Composable
fun HomeScreen(bridge: NativeBridge, darkTheme: Boolean, onOpen: (MiniApp) -> Unit) {
    val apps = miniApps.filter { it.section == MiniApp.Section.Apps }
    val tools = miniApps.filter { it.section == MiniApp.Section.Tools }
    val riseStore = remember { RiseStore.get(bridge) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp,
        ),
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        fullWidth { Header(bridge, darkTheme, onOpenRenewal = { onOpen(Fornyelse) }) }

        riseStore.pendingAwakeCheck?.let { title ->
            fullWidth { AwakeCard(title, onConfirm = riseStore::confirmAwake) }
        }

        fullWidth { SectionTitle("Appene dine") }
        items(apps) { app -> AppTile(app, darkTheme, onClick = { onOpen(app) }) }

        fullWidth { SectionTitle("Verktøy") }
        items(tools, span = { GridItemSpan(maxLineSpan) }) { app ->
            ToolRow(app, darkTheme, onClick = { onOpen(app) })
        }
    }
}

private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun Header(bridge: NativeBridge, darkTheme: Boolean, onOpenRenewal: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .bleedHorizontally(20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            .background(
                Brush.verticalGradient(
                    listOf(colors.tint(SalatColors.Leaf, darkTheme), colors.tint(SalatColors.Bowl, darkTheme)),
                ),
            )
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(84.dp).clip(CircleShape).background(SalatColors.Bowl),
            ) {
                Image(painterResource(Res.drawable.salat_logo), contentDescription = null, modifier = Modifier.size(66.dp))
            }
            Column {
                Text(greeting(), style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant)
                Text("Salat", style = MaterialTheme.typography.displaySmall, color = colors.onBackground)
            }
        }
        RenewalChip(bridge, onClick = onOpenRenewal)
    }
}

@Composable
private fun RenewalChip(bridge: NativeBridge, onClick: () -> Unit) {
    val timeLeft = remember { bridge.signingTimeLeft() } ?: return
    val urgent = timeLeft.inWholeDays < 2
    val text = when {
        timeLeft.isNegative() -> "Må fornyes i SideStore nå!"
        else -> "Fornyes om ${formatTimeLeft(timeLeft)}"
    }
    Surface(
        shape = CircleShape,
        color = if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        contentColor = if (urgent) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.clip(CircleShape).clickable(onClick = onClick),
    ) {
        Text(
            "⏳  $text",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 10.dp, start = 4.dp),
    )
}

@Composable
private fun AppTile(app: MiniApp, darkTheme: Boolean, onClick: () -> Unit) {
    val available = app.content != null
    val colors = MaterialTheme.colorScheme
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .pressable(enabled = available, onClick = onClick)
            .fillMaxWidth()
            .heightIn(min = 176.dp)
            .alpha(if (available) 1f else 0.75f)
            .clip(RoundedCornerShape(28.dp))
            .background(colors.tint(app.color, darkTheme))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            AppIcon(app, size = 60)
            Spacer(Modifier.weight(1f))
            if (!available) {
                Text(
                    "Snart",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.surface.copy(alpha = 0.7f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        Spacer(Modifier.weight(1f, fill = false).heightIn(min = 6.dp))
        Text(app.title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
        Text(
            app.description,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ToolRow(app: MiniApp, darkTheme: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .pressable(onClick = onClick)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surfaceContainer)
            .padding(12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(colors.tint(app.color, darkTheme)),
        ) {
            Text(app.emoji, fontSize = 22.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(app.title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            Text(app.description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        Text("›", fontSize = 26.sp, color = colors.onSurfaceVariant, modifier = Modifier.padding(end = 6.dp))
    }
}

@Composable
fun AppIcon(app: MiniApp, size: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.3f).dp))
            .background(app.color),
    ) {
        val icon = app.icon
        if (icon != null) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.size((size * 0.8f).dp))
        } else {
            Text(app.emoji, fontSize = (size * 0.5f).sp)
        }
    }
}

/** Krymper litt når man trykker, så det føles som en knapp. */
@Composable
private fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f)
    return scale(scale).clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled,
        onClick = onClick,
    )
}

/** Lar elementet gå [amount] ut over kantene på begge sider, forbi padding i listen. */
private fun Modifier.bleedHorizontally(amount: Dp): Modifier = layout { measurable, constraints ->
    val extra = (amount * 2).roundToPx()
    val placeable = measurable.measure(
        constraints.copy(minWidth = constraints.minWidth + extra, maxWidth = constraints.maxWidth + extra),
    )
    layout(placeable.width - extra, placeable.height) { placeable.place(-extra / 2, 0) }
}

private fun greeting(): String {
    val hour = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
    return when (hour) {
        in 5..9 -> "God morgen ☀️"
        in 10..11 -> "God formiddag 🌤️"
        in 12..17 -> "God ettermiddag 🌿"
        in 18..22 -> "God kveld 🌆"
        else -> "God natt 🌙"
    }
}
