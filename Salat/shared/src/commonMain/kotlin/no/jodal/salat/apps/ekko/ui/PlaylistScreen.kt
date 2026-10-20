package no.jodal.salat.apps.ekko.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import no.jodal.salat.apps.ekko.Clip
import no.jodal.salat.apps.ekko.EkkoPlayer
import no.jodal.salat.apps.ekko.EkkoStore
import no.jodal.salat.apps.ekko.Playlist
import no.jodal.salat.apps.ekko.formatCount

@Composable
fun PlaylistScreen(
    playlist: Playlist,
    store: EkkoStore,
    player: EkkoPlayer,
    onBack: () -> Unit,
    onMore: (Clip) -> Unit,
    onAddClips: () -> Unit,
    onPlaylistActions: () -> Unit,
) {
    val clips = store.clipsIn(playlist)
    val (glow, _) = EkkoPalette.cover(playlist.id)
    val source = EkkoPlayer.QueueSource(playlist.id, playlist.name)
    val playingThis = player.isFrom(source)

    Box(
        Modifier
            .fillMaxSize()
            .background(EkkoPalette.Ink)
            .background(Brush.verticalGradient(0f to glow.copy(alpha = 0.22f), 0.45f to EkkoPalette.Ink)),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = bottomSpace()),
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().height(52.dp).padding(horizontal = 4.dp),
                ) {
                    RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Tilbake", onBack, size = 38.dp)
                    Spacer(Modifier.weight(1f))
                    RoundIconButton(Icons.Rounded.MoreHoriz, "Mer", onPlaylistActions, size = 38.dp)
                }
            }
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 18.dp),
                ) {
                    PlaylistCover(playlist.id, clips, 188.dp)
                    Spacer(Modifier.height(10.dp))
                    Text(playlist.name, style = MaterialTheme.typography.headlineMedium, color = EkkoPalette.Text, textAlign = TextAlign.Center)
                    Text(formatCount(clips.size, clips.sumOf { it.lengthSeconds }), style = MaterialTheme.typography.bodyMedium, color = EkkoPalette.TextDim)
                }
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp, bottom = 10.dp),
                ) {
                    SecondaryButton("Legg til", onAddClips, icon = Icons.Rounded.Add, modifier = Modifier.height(44.dp))
                    Spacer(Modifier.weight(1f))
                    if (clips.isNotEmpty()) {
                        ShuffleButton(store.data.shuffle, player::toggleShuffle)
                        RoundIconButton(
                            if (playingThis && player.isPlaying) Icons.Rounded.Pause
                            else Icons.Rounded.PlayArrow,
                            if (playingThis && player.isPlaying) "Pause" else "Spill av",
                            onClick = { player.playOrPause(clips, source) },
                            size = 52.dp,
                            background = EkkoPalette.Lime,
                            tint = EkkoPalette.OnLime,
                            iconSize = 30.dp,
                        )
                    }
                }
            }
            if (clips.isEmpty()) {
                item {
                    Text(
                        "Ingen lyder her ennå. Trykk «Legg til» for å velge lyder.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EkkoPalette.TextDim,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 32.dp),
                    )
                }
            }
            items(clips, key = { it.id }) { clip ->
                ClipRow(
                    clip = clip,
                    isCurrent = playingThis && player.current?.id == clip.id,
                    isPlaying = player.isPlaying,
                    onClick = { player.playClip(clips, clip, source) },
                    onMore = { onMore(clip) },
                )
            }
        }
    }
}
