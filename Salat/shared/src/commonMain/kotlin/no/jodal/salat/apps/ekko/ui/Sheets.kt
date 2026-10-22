package no.jodal.salat.apps.ekko.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import no.jodal.salat.Haptics
import no.jodal.salat.apps.ekko.Clip
import no.jodal.salat.apps.ekko.EkkoStore
import no.jodal.salat.apps.ekko.Playlist
import no.jodal.salat.apps.ekko.formatCount
import no.jodal.salat.apps.ekko.formatLength

/** Et ark nederst i Ekko sin stil. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EkkoSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = EkkoPalette.Surface,
        contentColor = EkkoPalette.Text,
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 6.dp).size(width = 36.dp, height = 5.dp).clip(CircleShape).background(EkkoPalette.Line))
        },
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(start = 12.dp, end = 12.dp, bottom = 16.dp), content = content)
    }
}

@Composable
fun ClipActionsSheet(
    clip: Clip,
    playlist: Playlist?,
    onSoundboard: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onToggleSoundboard: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRemoveFromPlaylist: () -> Unit,
    onDelete: () -> Unit,
) {
    EkkoSheet(onDismiss) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            ClipCover(clip, 52.dp)
            Column(Modifier.weight(1f)) {
                Text(clip.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(clip.artist.ifBlank { null }, formatLength(clip.lengthSeconds)).joinToString("  ·  "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = EkkoPalette.TextDim,
                )
            }
        }
        Hairline(Modifier.padding(vertical = 8.dp))
        SheetAction(Icons.Rounded.ContentCut, "Klipp og endre navn", onEdit)
        SheetAction(Icons.AutoMirrored.Rounded.PlaylistAdd, "Legg til i spilleliste", onAddToPlaylist)
        SheetAction(
            Icons.Rounded.Widgets,
            if (onSoundboard) "Fjern fra lydbrettet" else "Legg på lydbrettet i widgeten",
            onToggleSoundboard,
        )
        if (playlist != null) {
            SheetAction(Icons.Rounded.RemoveCircleOutline, "Fjern fra «${playlist.name}»", onRemoveFromPlaylist)
        }
        SheetAction(Icons.Rounded.DeleteOutline, "Slett lyden", onDelete, color = EkkoPalette.Danger)
    }
}

/** Velg en spilleliste å legge lyden i, eller lag en ny. */
@Composable
fun AddToPlaylistSheet(clip: Clip, store: EkkoStore, onDismiss: () -> Unit) {
    var naming by remember { mutableStateOf(false) }
    EkkoSheet(onDismiss) {
        if (naming) {
            NameForm("Ny spilleliste", "", "Lag og legg til") { name ->
                store.createPlaylist(name, listOf(clip.id))
                onDismiss()
            }
            return@EkkoSheet
        }
        Text("Legg til i spilleliste", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        LazyColumn(Modifier.heightIn(max = 420.dp)) {
            item {
                PickRow(
                    leading = {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(EkkoPalette.SurfaceHigh)) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = EkkoPalette.Lime)
                        }
                    },
                    title = "Ny spilleliste",
                    subtitle = null,
                    checked = false,
                    onClick = { naming = true },
                )
            }
            items(store.data.playlists.sortedByDescending { it.createdAtMillis }, key = { it.id }) { playlist ->
                val clips = store.clipsIn(playlist)
                val already = clip.id in playlist.clipIds
                PickRow(
                    leading = { PlaylistCover(playlist.id, clips, 48.dp) },
                    title = playlist.name,
                    subtitle = if (already) "Er alt her" else formatCount(clips.size, clips.sumOf { it.lengthSeconds }),
                    checked = already,
                    onClick = {
                        if (!already) store.addToPlaylist(playlist.id, listOf(clip.id))
                        onDismiss()
                    },
                )
            }
        }
    }
}

/** Velg lyder å legge i en spilleliste. */
@Composable
fun PickClipsSheet(library: List<Clip>, alreadyIn: Set<String>, onDismiss: () -> Unit, onAdd: (List<String>) -> Unit) {
    var chosen by remember { mutableStateOf(emptySet<String>()) }
    EkkoSheet(onDismiss) {
        Text("Legg til lyder", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        val available = library.filter { it.id !in alreadyIn }
        if (available.isEmpty()) {
            Text(
                if (library.isEmpty()) "Du har ingen lyder ennå." else "Alle lydene dine er alt i spillelisten.",
                style = MaterialTheme.typography.bodyMedium,
                color = EkkoPalette.TextDim,
                modifier = Modifier.padding(12.dp),
            )
            return@EkkoSheet
        }
        LazyColumn(Modifier.heightIn(max = 460.dp)) {
            items(available, key = { it.id }) { clip ->
                val checked = clip.id in chosen
                PickRow(
                    leading = { ClipCover(clip, 48.dp) },
                    title = clip.title,
                    subtitle = listOfNotNull(clip.artist.ifBlank { null }, formatLength(clip.lengthSeconds)).joinToString("  ·  "),
                    checked = checked,
                    onClick = { chosen = if (checked) chosen - clip.id else chosen + clip.id },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            if (chosen.isEmpty()) "Velg lyder" else "Legg til ${chosen.size}",
            // Samme rekkefølge som i biblioteket, ikke rekkefølgen de ble valgt i.
            { onAdd(available.map { it.id }.filter { it in chosen }) },
            enabled = chosen.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun PickRow(leading: @Composable () -> Unit, title: String, subtitle: String?, checked: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).pressable(onClick, pressedScale = 0.98f, haptic = Haptics::select).padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        leading()
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = EkkoPalette.TextDim, maxLines = 1)
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .then(if (checked) Modifier.background(EkkoPalette.Lime) else Modifier.border(2.dp, EkkoPalette.Line, CircleShape)),
        ) {
            if (checked) Icon(Icons.Rounded.Check, contentDescription = null, tint = EkkoPalette.OnLime, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun PlaylistActionsSheet(playlist: Playlist, onDismiss: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    EkkoSheet(onDismiss) {
        Text(playlist.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        SheetAction(Icons.Rounded.Edit, "Endre navn", onRename)
        SheetAction(Icons.Rounded.DeleteOutline, "Slett spillelisten", onDelete, color = EkkoPalette.Danger)
    }
}

/** Gi noe et navn, f.eks. en ny spilleliste. */
@Composable
fun NameSheet(title: String, initial: String, confirm: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    EkkoSheet(onDismiss) {
        NameForm(title, initial, confirm, onConfirm)
    }
}

@Composable
private fun NameForm(title: String, initial: String, confirm: String, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        EkkoTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = "Navn på spillelisten",
            onDone = { if (name.isNotBlank()) onConfirm(name) },
            modifier = Modifier.focusRequester(focus),
        )
        PrimaryButton(confirm, { onConfirm(name) }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth())
    }
}

/** Spør før noe slettes. */
@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(EkkoPalette.SurfaceHigh)
                .padding(22.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = EkkoPalette.Text)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = EkkoPalette.TextDim)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillAction("Avbryt", EkkoPalette.Surface, EkkoPalette.Text, onDismiss, Modifier.weight(1f))
                PillAction(confirm, EkkoPalette.Danger, EkkoPalette.Ink, onConfirm, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PillAction(text: String, background: Color, color: Color, onClick: () -> Unit, modifier: Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.height(48.dp).clip(CircleShape).background(background).pressable(onClick),
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = color)
    }
}
