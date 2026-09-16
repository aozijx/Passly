package com.aozijx.passly.presentation.feature.vault.detail.binding

import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.presentation.feature.vault.detail.DetailEditKey
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiState
import com.aozijx.passly.presentation.ui.vault.detail.component.NotesSection
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailNotesUiModel

@Composable
internal fun DetailNotesBinding(
    entry: Entry,
    uiState: DetailUiState,
    onAction: (DetailUiAction) -> Unit,
) {
    NotesSection(
        model = DetailNotesUiModel(
            notes = entry.secret.notes,
            editedNotes = uiState.fieldEdits.draft(DetailEditKey.NOTES),
            isEditing = uiState.fieldEdits.isEditing(DetailEditKey.NOTES),
        ),
        onEditStarted = { onAction(DetailUiAction.StartNotesEdit) },
        onNotesChanged = { onAction(DetailUiAction.UpdateNotesDraft(it)) },
        onNotesSaved = { onAction(DetailUiAction.SaveNotes) },
    )
}
