package no.jodal.salat.apps.rise.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import no.jodal.salat.apps.rise.RiseGroup
import no.jodal.salat.apps.rise.RiseStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsSheet(store: RiseStore, onDismiss: () -> Unit) {
    var groups by remember { mutableStateOf(store.data.groups) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun close(save: () -> Unit = {}) {
        save()
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = { close() },
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            TextButton(onClick = { close() }) { Text("Avbryt", style = MaterialTheme.typography.bodyLarge) }
            Text("Grupper", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            TextButton(onClick = {
                close {
                    val cleaned = groups.map { it.copy(name = it.name.trim().ifEmpty { "Uten navn" }) }
                    store.update { data ->
                        // Alarmer i slettede grupper blir liggende uten gruppe.
                        val ids = cleaned.map { it.id }.toSet()
                        data.copy(
                            groups = cleaned,
                            alarms = data.alarms.map { if (it.groupId == null || it.groupId in ids) it else it.copy(groupId = null) },
                        )
                    }
                }
            }) {
                Text("Lagre", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
        }
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsSection(null) {
                groups.forEachIndexed { index, group ->
                    if (index > 0) SectionDivider()
                    GroupRow(
                        group = group,
                        canDelete = groups.size > 1,
                        canMoveUp = index > 0,
                        canMoveDown = index < groups.lastIndex,
                        onChange = { changed -> groups = groups.map { if (it.id == group.id) changed else it } },
                        onMove = { offset ->
                            groups = groups.toMutableList().apply { add(index + offset, removeAt(index)) }
                        },
                        onDelete = { groups = groups.filter { it.id != group.id } },
                    )
                }
            }
            TextButton(
                onClick = { groups = groups + RiseGroup(id = randomId(), name = "") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("+  Ny gruppe", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "Gruppene vises i denne rekkefølgen. Alarmer i en gruppe du sletter, blir liggende uten gruppe.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Box(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun GroupRow(
    group: RiseGroup,
    canDelete: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onChange: (RiseGroup) -> Unit,
    onMove: (offset: Int) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BasicTextField(
            value = group.name,
            onValueChange = { onChange(group.copy(name = it)) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (group.name.isEmpty()) Text("Navn på gruppen", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                    inner()
                }
            },
        )
        Row {
            IconButton(onClick = { onMove(-1) }, enabled = canMoveUp, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Flytt opp")
            }
            IconButton(onClick = { onMove(1) }, enabled = canMoveDown, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Flytt ned")
            }
        }
        if (canDelete) {
            TextButton(onClick = onDelete) { Text("Slett", color = colors.error) }
        }
    }
}
