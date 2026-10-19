package no.jodal.salat.apps.kompis.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import no.jodal.salat.NativeBridge
import no.jodal.salat.apps.kompis.Conversation
import no.jodal.salat.apps.kompis.KompisStore
import no.jodal.salat.apps.kompis.Period
import no.jodal.salat.apps.kompis.formatDuration
import no.jodal.salat.apps.kompis.formatWhen
import no.jodal.salat.apps.kompis.periodOf
import no.jodal.salat.ui.BackButton
import no.jodal.salat.ui.LocalDarkTheme

private sealed interface KompisDestination {
    data object Home : KompisDestination
    data class Chat(val id: String) : KompisDestination
    data class Live(val conversationId: String?) : KompisDestination
}

@Composable
fun KompisScreen(bridge: NativeBridge, onBack: () -> Unit) {
    KompisTheme {
        val store = remember { KompisStore.get(bridge) }
        var destination by remember { mutableStateOf<KompisDestination>(KompisDestination.Home) }
        var showSettings by remember { mutableStateOf(false) }
        var showTranscript by remember { mutableStateOf(false) }

        AnimatedContent(
            targetState = destination,
            transitionSpec = {
                val duration = 300
                when {
                    targetState is KompisDestination.Live ->
                        slideInVertically(tween(duration)) { it } togetherWith fadeOut(tween(duration))
                    initialState is KompisDestination.Live ->
                        fadeIn(tween(duration)) togetherWith slideOutVertically(tween(duration)) { it }
                    targetState is KompisDestination.Chat ->
                        slideInHorizontally(tween(duration)) { it } togetherWith fadeOut(tween(duration))
                    else -> fadeIn(tween(duration)) togetherWith slideOutHorizontally(tween(duration)) { it }
                }
            },
        ) { current ->
            when (current) {
                KompisDestination.Home -> KompisHome(
                    store = store,
                    onBack = onBack,
                    onStart = { destination = KompisDestination.Live(null) },
                    onOpen = { destination = KompisDestination.Chat(it.id) },
                    onSettings = { showSettings = true },
                )
                is KompisDestination.Chat -> {
                    val conversation = store.conversation(current.id)
                    if (conversation == null) {
                        destination = KompisDestination.Home
                    } else {
                        ConversationScreen(
                            conversation = conversation,
                            nowMillis = store.nowMillis(),
                            onBack = { destination = KompisDestination.Home },
                            onContinue = { destination = KompisDestination.Live(conversation.id) },
                            onDelete = {
                                store.deleteConversation(conversation.id)
                                destination = KompisDestination.Home
                            },
                        )
                    }
                }
                is KompisDestination.Live -> {
                    val conversation = current.conversationId?.let(store::conversation)
                    LiveScreen(
                        state = placeholderLiveState(store, conversation),
                        showTranscript = showTranscript,
                        onToggleTranscript = { showTranscript = !showTranscript },
                        onToggleMute = {},
                        onEnd = { destination = conversation?.let { KompisDestination.Chat(it.id) } ?: KompisDestination.Home },
                        onMinimize = { destination = conversation?.let { KompisDestination.Chat(it.id) } ?: KompisDestination.Home },
                    )
                }
            }
        }

        if (showSettings) {
            KompisSettingsSheet(store = store, onDismiss = { showSettings = false })
        }
    }
}

/** Det samtaleskjermen viser før koblingen til Gemini er på plass. */
private fun placeholderLiveState(store: KompisStore, conversation: Conversation?) = LiveUiState(
    title = conversation?.title ?: "Ny samtale",
    status = if (store.hasApiKey) LiveStatus.NotAvailable else LiveStatus.NeedsApiKey,
    messages = conversation?.messages.orEmpty(),
    message = if (store.hasApiKey) {
        "Samtaler med Gemini kommer i neste versjon av Kompis."
    } else {
        "Legg inn en Gemini-nøkkel i innstillingene for å snakke med Kompis."
    },
)

@Composable
fun KompisHome(
    store: KompisStore,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onOpen: (Conversation) -> Unit,
    onSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val now = store.nowMillis()
    val conversations = store.data.conversations.sortedByDescending { it.updatedAtMillis }
    val grouped = conversations.groupBy { periodOf(it.updatedAtMillis, now) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "top") { HomeTopBar(onBack = onBack, onSettings = onSettings) }
        item(key = "welcome") { WelcomeCard(onStart = onStart) }
        if (!store.hasApiKey) {
            item(key = "key") { ApiKeyCard(onClick = onSettings) }
        }

        if (conversations.isEmpty()) {
            item(key = "empty") { EmptyConversations() }
        }
        for (period in Period.entries) {
            val items = grouped[period] ?: continue
            item(key = "period-$period") {
                Text(
                    period.label.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 1.5.sp,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp, top = 18.dp, bottom = 2.dp),
                )
            }
            items(items, key = { it.id }) { ConversationRow(it, now, onClick = { onOpen(it) }) }
        }
    }
}

@Composable
private fun HomeTopBar(onBack: () -> Unit, onSettings: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth().statusBarsPadding().height(56.dp)) {
        Box(Modifier.align(Alignment.CenterStart)) { BackButton(onBack) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(28.dp).clip(CircleShape).background(KompisPalette.welcome(LocalDarkTheme.current)),
            ) {
                Icon(Icons.Rounded.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
            }
            Text("Kompis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.onBackground)
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(36.dp)
                .clip(CircleShape)
                .background(colors.surfaceVariant)
                .clickable(onClick = onSettings),
        ) {
            Icon(Icons.Rounded.Settings, contentDescription = "Innstillinger", tint = colors.onSurface, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun WelcomeCard(onStart: () -> Unit) {
    val white = Color.White
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(KompisPalette.welcome(LocalDarkTheme.current)),
    ) {
        Orb(
            mode = OrbMode.Idle,
            level = 0f,
            // Kula titter fram fra hjørnet.
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 44.dp, y = (-34).dp).size(210.dp),
        )
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Hei!", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = white)
            Text(
                "Hva vil du snakke\nom i dag?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Normal,
                color = white.copy(alpha = 0.9f),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .padding(top = 18.dp)
                    .clip(CircleShape)
                    .background(white)
                    .clickable(onClick = onStart)
                    .padding(start = 18.dp, end = 22.dp, top = 14.dp, bottom = 14.dp),
            ) {
                Icon(Icons.Rounded.Mic, contentDescription = null, tint = Color(0xFF5B45C9), modifier = Modifier.size(22.dp))
                Text("Start samtale", style = MaterialTheme.typography.titleMedium, color = Color(0xFF231A5C))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 14.dp)) {
                FeatureChip(Icons.Rounded.Headphones, "Med AirPods")
                FeatureChip(Icons.Rounded.Lock, "Med skjermen av")
            }
        }
    }
}

@Composable
private fun FeatureChip(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}

@Composable
private fun ApiKeyCard(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.primaryContainer)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Icon(Icons.Rounded.Key, contentDescription = null, tint = colors.onPrimaryContainer)
        Column(Modifier.weight(1f)) {
            Text("Legg inn Gemini-nøkkel", style = MaterialTheme.typography.titleMedium, color = colors.onPrimaryContainer)
            Text(
                "Kompis trenger en nøkkel fra Google AI Studio for å snakke.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onPrimaryContainer.copy(alpha = 0.8f),
            )
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.onPrimaryContainer)
    }
}

@Composable
private fun EmptyConversations() {
    val colors = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 36.dp, start = 24.dp, end = 24.dp),
    ) {
        Icon(Icons.Rounded.Forum, contentDescription = null, tint = colors.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
        Text("Ingen samtaler ennå", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
        Text(
            "Samtalene dine lagres her, så du kan lese dem og fortsette der dere slapp.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ConversationRow(conversation: Conversation, nowMillis: Long, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 10.dp, top = 14.dp, bottom = 14.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                conversation.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            conversation.messages.lastOrNull()?.let {
                Text(
                    it.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "${formatWhen(conversation.updatedAtMillis, nowMillis).replaceFirstChar { it.uppercase() }}  ·  ${formatDuration(conversation.durationSeconds)}",
                style = MaterialTheme.typography.labelMedium,
                color = colors.primary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.onSurfaceVariant)
    }
}
