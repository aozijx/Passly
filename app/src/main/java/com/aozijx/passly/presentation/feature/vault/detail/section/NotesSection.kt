package com.aozijx.passly.presentation.feature.vault.detail.section

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
import com.aozijx.passly.core.ui.components.PasslyOutlinedTextField
import com.aozijx.passly.core.ui.components.markdown.PasslyMarkdownDocument
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.presentation.feature.vault.detail.EntryEditState
import com.aozijx.passly.presentation.feature.vault.detail.component.InfoGroupCard

@Composable
fun NotesSection(
    entry: Entry,
    editState: EntryEditState,
    onEntryUpdated: (Entry) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val notesLabel = stringResource(R.string.field_notes)
    val addNotesPlaceholder = stringResource(R.string.vault_detail_add_notes)
    val noNotesLabel = stringResource(R.string.vault_detail_no_notes)

    InfoGroupCard(title = notesLabel) {
        if (editState.isEditingNotes) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PasslyOutlinedTextField(
                    value = editState.editedNotes,
                    onValueChange = { editState.editedNotes = it },
                    label = notesLabel,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
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
                    Text(stringResource(R.string.save))
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            editState.startNotesEditing(entry.resolveNotes())
                        },
                        onClick = { /* 不做任何事，只有长按触发 */ }
                    )
                    .padding(16.dp)
            ) {
                PasslyMarkdownDocument(
                    content = entry.resolveNotes(),
                    modifier = Modifier.fillMaxWidth(),
                    emptyContent = {
                        Text(text = noNotesLabel, style = MaterialTheme.typography.bodyMedium)
                    }
                )
            }
        }
    }
}

private fun Entry.resolveNotes(): String? = secret.notes
