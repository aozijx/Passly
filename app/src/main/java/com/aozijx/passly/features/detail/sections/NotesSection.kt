package com.aozijx.passly.features.detail.sections

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.features.detail.EntryEditState
import com.aozijx.passly.features.detail.components.InfoGroupCard
import com.aozijx.passly.features.vault.VaultViewModel

@Composable
fun NotesSection(
    entry: VaultEntry,
    editState: EntryEditState,
    viewModel: VaultViewModel,
    onEntryUpdated: (VaultEntry) -> Unit = viewModel::updateVaultEntry
) {
    val haptic = LocalHapticFeedback.current
    val notesLabel = stringResource(R.string.vault_detail_notes)
    val addNotesPlaceholder = stringResource(R.string.vault_detail_add_notes)
    val noNotesLabel = stringResource(R.string.vault_detail_no_notes)

    InfoGroupCard(title = notesLabel) {
        if (editState.isEditingNotes) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = editState.editedNotes,
                    onValueChange = { editState.editedNotes = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 10,
                    placeholder = { Text(addNotesPlaceholder) }
                )
                TextButton(
                    onClick = {
                        val updatedEntry = editState.applyNotesOnly(entry)
                        onEntryUpdated(updatedEntry)
                        editState.isEditingNotes = false
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.action_save))
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            editState.editedNotes = entry.notes ?: ""
                            editState.isEditingNotes = true
                        },
                        onClick = { /* 不做任何事，只有长按触发 */ }
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = entry.notes ?: noNotesLabel,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}