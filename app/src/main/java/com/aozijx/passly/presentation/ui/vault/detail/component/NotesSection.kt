package com.aozijx.passly.presentation.ui.vault.detail.component

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
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailNotesUiModel

@Composable
fun NotesSection(
    model: DetailNotesUiModel,
    onEditStarted: () -> Unit,
    onNotesChanged: (String) -> Unit,
    onNotesSaved: (String) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val notesLabel = stringResource(R.string.field_notes)
    val addNotesPlaceholder = stringResource(R.string.vault_detail_add_notes)
    val noNotesLabel = stringResource(R.string.vault_detail_no_notes)

    InfoGroupCard(title = notesLabel) {
        if (model.isEditing) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PasslyOutlinedTextField(
                    value = model.editedNotes,
                    onValueChange = onNotesChanged,
                    label = notesLabel,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 3,
                    maxLines = 10,
                    placeholder = { Text(addNotesPlaceholder) }
                )
                TextButton(
                    onClick = {
                        onNotesSaved(model.editedNotes)
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
                            onEditStarted()
                        },
                        onClick = { /* 不做任何事，只有长按触发 */ }
                    )
                    .padding(16.dp)
            ) {
                PasslyMarkdownDocument(
                    content = model.notes,
                    modifier = Modifier.fillMaxWidth(),
                    emptyContent = {
                        Text(text = noNotesLabel, style = MaterialTheme.typography.bodyMedium)
                    }
                )
            }
        }
    }
}
