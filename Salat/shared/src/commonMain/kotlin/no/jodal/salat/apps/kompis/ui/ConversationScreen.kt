package no.jodal.salat.apps.kompis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import no.jodal.salat.apps.kompis.Conversation
import no.jodal.salat.apps.kompis.formatDuration
import no.jodal.salat.apps.kompis.formatWhen

/** En lagret samtale, som en chat. */
@Composable
fun ConversationScreen(
    conversation: Conversation,
    nowMillis: Long,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var confirmDelete by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 16.dp),
            ) {
                CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Tilbake", onClick = onBack)
                Spacer(Modifier.weight(1f))
                CircleIconButton(Icons.Rounded.Edit, "Endre navn", onClick = { renaming = true })
                CircleIconButton(Icons.Rounded.DeleteOutline, "Slett samtalen", onClick = { confirmDelete = true })
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 130.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                        Text(
                            conversation.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.onBackground,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { renaming = true },
                        )
                        Text(
                            "${formatWhen(conversation.createdAtMillis, nowMillis).replaceFirstChar { it.uppercase() }}  ·  ${formatDuration(conversation.durationSeconds)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
                items(conversation.messages) { ChatMessage(it.role, it.text, dark = false) }
            }
        }

        // «Fortsett samtalen» nederst, over en myk overgang.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, colors.background, colors.background)))
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 32.dp, bottom = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(CircleShape)
                    .background(colors.primary)
                    .clickable(onClick = onContinue),
            ) {
                VoiceBars(level = 1f, color = colors.onPrimary, animated = false, modifier = Modifier.height(18.dp))
                Text("Fortsett samtalen", style = MaterialTheme.typography.titleMedium, color = colors.onPrimary)
            }
        }
    }

    if (renaming) {
        RenameDialog(
            current = conversation.title,
            onDismiss = { renaming = false },
            onSave = {
                renaming = false
                onRename(it)
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Slette samtalen?") },
            text = { Text("«${conversation.title}» blir borte for godt.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Slett", color = colors.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Avbryt") } },
        )
    }
}

@Composable
private fun RenameDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(TextFieldValue(current, selection = TextRange(0, current.length))) }
    val focus = remember { FocusRequester() }
    val canSave = value.text.isNotBlank()
    LaunchedEffect(Unit) { focus.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Endre navn") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                placeholder = { Text("Navn på samtalen") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (canSave) onSave(value.text) }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(value.text) }, enabled = canSave) { Text("Lagre") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Avbryt") } },
    )
}
