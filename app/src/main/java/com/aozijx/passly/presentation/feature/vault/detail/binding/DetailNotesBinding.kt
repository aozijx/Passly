package com.aozijx.passly.presentation.feature.vault.detail.binding

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.TextFieldValue
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.presentation.feature.vault.detail.DetailEditCompletion
import com.aozijx.passly.feature.vault.detail.DetailEntryPatch
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.EntryEditState
import com.aozijx.passly.presentation.ui.vault.detail.component.NotesSection
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailNotesUiModel

@Composable
internal fun DetailNotesBinding(
    entry: Entry,
    editState: EntryEditState,
    onAction: (DetailUiAction) -> Unit,
) {
    NotesSection(
        model = DetailNotesUiModel(
            notes = entry.secret.notes,
            editedNotes = editState.editedNotes.text,
            isEditing = editState.isEditingNotes,
        ),
        onEditStarted = { editState.startNotesEditing(entry.secret.notes) },
        onNotesChanged = { editState.editedNotes = TextFieldValue(it) },
        onNotesSaved = {
            editState.editedNotes = TextFieldValue(it)
            onAction(
                DetailUiAction.CommitPatch(
                    DetailEntryPatch.Notes(it.ifBlank { null }),
                    DetailEditCompletion.Notes,
                ),
            )
        },
    )
}
