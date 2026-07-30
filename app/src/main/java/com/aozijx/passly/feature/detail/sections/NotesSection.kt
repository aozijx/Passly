package com.aozijx.passly.feature.detail.sections

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
import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.feature.detail.components.InfoGroupCard
import com.aozijx.passly.feature.detail.internal.EntryEditState
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography

@Composable
fun NotesSection(
    entry: VaultEntry,
    editState: EntryEditState,
    onEntryUpdated: (VaultEntry) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val notesLabel = stringResource(R.string.remark)
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
                            editState.editedNotes = entry.resolveNotes() ?: ""
                            editState.isEditingNotes = true
                        },
                        onClick = { /* 不做任何事，只有长按触发 */ }
                    )
                    .padding(16.dp)
            ) {
                val notes = entry.resolveNotes()
                if (notes.isNullOrBlank()) {
                    Text(text = noNotesLabel, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Markdown(
                        content = notes,
                        modifier = Modifier.fillMaxWidth(),
                        typography = markdownTypography(
                            h1 = MaterialTheme.typography.headlineLarge,
                            h2 = MaterialTheme.typography.headlineMedium,
                            h3 = MaterialTheme.typography.headlineSmall,
                            h4 = MaterialTheme.typography.titleLarge,
                            h5 = MaterialTheme.typography.titleMedium,
                            h6 = MaterialTheme.typography.titleSmall,
                            text = MaterialTheme.typography.bodyMedium,
                            paragraph = MaterialTheme.typography.bodyMedium,
                            ordered = MaterialTheme.typography.bodyMedium,
                            bullet = MaterialTheme.typography.bodyMedium,
                            list = MaterialTheme.typography.bodyMedium
                        )
                    )
                }
            }
        }
    }
}

private fun VaultEntry.resolveNotes(): String? = secret.notes
