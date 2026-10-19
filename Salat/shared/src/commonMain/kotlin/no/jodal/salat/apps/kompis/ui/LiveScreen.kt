package no.jodal.salat.apps.kompis.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import no.jodal.salat.apps.kompis.Message
import no.jodal.salat.apps.kompis.Role
import no.jodal.salat.apps.kompis.formatTimer

/** Hvor samtalen er akkurat nå. */
enum class LiveStatus(val label: String, val orb: OrbMode) {
    NeedsApiKey("Mangler nøkkel", OrbMode.Idle),
    Connecting("Kobler til", OrbMode.Thinking),
    Listening("Lytter", OrbMode.Listening),
    Thinking("Tenker", OrbMode.Thinking),
    Speaking("Snakker", OrbMode.Speaking),
    Reconnecting("Kobler til igjen", OrbMode.Thinking),
    Muted("Mikrofonen er av", OrbMode.Idle),
    Failed("Noe gikk galt", OrbMode.Idle),
    Ended("Samtalen er avsluttet", OrbMode.Idle),
}

/** Alt samtaleskjermen viser. Fylles av koblingen til Gemini. */
data class LiveUiState(
    val title: String = "Ny samtale",
    val status: LiveStatus = LiveStatus.Connecting,
    val elapsedSeconds: Int = 0,
    /** Lydnivået til den som snakker, fra 0 til 1. */
    val level: Float = 0f,
    val messages: List<Message> = emptyList(),
    /** Det du sier akkurat nå, før det er ferdig. */
    val userDraft: String? = null,
    /** Det Kompis sier akkurat nå, før det er ferdig. */
    val kompisDraft: String? = null,
    val muted: Boolean = false,
    /** Hvor lyden går, f.eks. «AirPods Pro» eller «Høyttaler». */
    val audioRoute: String = "iPhone",
    val message: String? = null,
)

@Composable
fun LiveScreen(
    state: LiveUiState,
    showTranscript: Boolean,
    onToggleTranscript: () -> Unit,
    onToggleMute: () -> Unit,
    onEnd: () -> Unit,
    onMinimize: () -> Unit,
) {
    val white = Color.White
    Box(Modifier.fillMaxSize().background(KompisPalette.Night)) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Topplinje
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                GlassButton(Icons.Rounded.KeyboardArrowDown, "Lukk", onClick = onMinimize)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        state.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = white,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(formatTimer(state.elapsedSeconds), style = MaterialTheme.typography.labelLarge, color = white.copy(alpha = 0.55f))
                }
                GlassButton(Icons.AutoMirrored.Rounded.Notes, if (showTranscript) "Skjul tekst" else "Vis tekst", selected = showTranscript, onClick = onToggleTranscript)
            }

            AnimatedContent(
                targetState = showTranscript,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { transcript ->
                if (transcript) {
                    Transcript(state)
                } else {
                    OrbStage(state)
                }
            }

            Controls(state, onToggleMute = onToggleMute, onEnd = onEnd)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 18.dp, bottom = 12.dp),
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = white.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                Text(
                    "Du kan låse skjermen. Kompis lytter videre.",
                    style = MaterialTheme.typography.labelMedium,
                    color = white.copy(alpha = 0.45f),
                )
            }
        }
    }
}

@Composable
private fun OrbStage(state: LiveUiState) {
    val white = Color.White
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
    ) {
        StatusPill(state.status)
        Orb(
            mode = if (state.muted) OrbMode.Idle else state.status.orb,
            level = state.level,
            modifier = Modifier.size(300.dp),
        )
        // Det som sies akkurat nå: det du sier mens Kompis lytter, ellers det Kompis sier.
        val userIsTalking = state.userDraft != null && state.status != LiveStatus.Speaking
        val caption = when {
            state.message != null -> state.message
            userIsTalking -> state.userDraft
            else -> state.kompisDraft ?: state.messages.lastOrNull { it.role == Role.Kompis }?.text
        }
        Text(
            caption.orEmpty(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Normal,
            color = if (userIsTalking && state.message == null) KompisPalette.Aqua else white,
            textAlign = TextAlign.Center,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 520.dp).height(124.dp),
        )
    }
}

@Composable
private fun StatusPill(status: LiveStatus) {
    val dot by animateColorAsState(
        when (status.orb) {
            OrbMode.Listening -> KompisPalette.Aqua
            OrbMode.Speaking -> KompisPalette.Orchid
            OrbMode.Thinking -> KompisPalette.Indigo
            OrbMode.Idle -> Color.White.copy(alpha = 0.4f)
        },
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
        Text(status.label, style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
    }
}

@Composable
private fun Transcript(state: LiveUiState) {
    val listState = rememberLazyListState()
    val count = state.messages.size + listOfNotNull(state.userDraft, state.kompisDraft).size
    LaunchedEffect(count) { if (count > 0) listState.animateScrollToItem(count - 1) }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(state.messages) { Bubble(it.role, it.text, dark = true) }
        state.userDraft?.let { item { Bubble(Role.User, it, dark = true, draft = true) } }
        state.kompisDraft?.let { item { Bubble(Role.Kompis, it, dark = true, draft = true) } }
    }
}

@Composable
private fun Controls(state: LiveUiState, onToggleMute: () -> Unit, onEnd: () -> Unit) {
    Row(
        // Bunnjustert, så teksten under knappene står på linje selv om Avslutt er større.
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        RoundControl(
            icon = if (state.muted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
            label = if (state.muted) "Slå på mikrofon" else "Demp",
            background = if (state.muted) Color.White else Color.White.copy(alpha = 0.12f),
            tint = if (state.muted) Color(0xFF120F26) else Color.White,
            size = 64,
            onClick = onToggleMute,
        )
        RoundControl(Icons.Rounded.CallEnd, "Avslutt", background = KompisPalette.EndCall, tint = Color.White, size = 78, onClick = onEnd)
        RoundControl(
            icon = if (state.audioRoute.contains("AirPods", ignoreCase = true)) Icons.Rounded.Headphones else Icons.Rounded.GraphicEq,
            label = state.audioRoute,
            background = Color.White.copy(alpha = 0.12f),
            tint = Color.White,
            size = 64,
            onClick = null,
        )
    }
}

@Composable
private fun RoundControl(icon: ImageVector, label: String, background: Color, tint: Color, size: Int, onClick: (() -> Unit)?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(background)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size((size * 0.42f).dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.65f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 90.dp),
        )
    }
}

@Composable
private fun GlassButton(icon: ImageVector, description: String, selected: Boolean = false, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (selected) 0.9f else 0.1f))
            .clickable(onClick = onClick),
    ) {
        Icon(icon, contentDescription = description, tint = if (selected) Color(0xFF120F26) else Color.White, modifier = Modifier.size(24.dp))
    }
}

/** En snakkeboble. Brukes både på samtaleskjermen (mørk) og i samtalehistorikken. */
@Composable
fun Bubble(role: Role, text: String, dark: Boolean, draft: Boolean = false) {
    val isUser = role == Role.User
    val colors = MaterialTheme.colorScheme
    val background = when {
        dark && isUser -> KompisPalette.Aqua.copy(alpha = 0.16f)
        dark -> Color.White.copy(alpha = 0.08f)
        isUser -> colors.primary
        else -> colors.surfaceContainer
    }
    val content = when {
        dark -> Color.White
        isUser -> colors.onPrimary
        else -> colors.onSurface
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = content.copy(alpha = if (draft) 0.6f else 1f),
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 22.dp,
                        topEnd = 22.dp,
                        bottomStart = if (isUser) 22.dp else 6.dp,
                        bottomEnd = if (isUser) 6.dp else 22.dp,
                    ),
                )
                .background(background)
                .padding(horizontal = 16.dp, vertical = 11.dp),
        )
    }
}
