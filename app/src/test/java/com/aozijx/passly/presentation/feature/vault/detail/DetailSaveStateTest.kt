package com.aozijx.passly.presentation.feature.vault.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailSaveStateTest {

    @Test
    fun saveStartedClearsOldErrorAndMarksOnlyRequestedEditAsSaving() {
        val state = DetailUiState(
            saveState = DetailSaveState.Failed(DetailEditCompletion.Title, "OLD_ERROR"),
        )

        val actual = DetailReducer.reduce(
            state,
            DetailMutation.SaveStarted(DetailEditCompletion.Notes),
        )

        assertEquals(
            DetailSaveState.Saving(DetailEditCompletion.Notes),
            actual.saveState,
        )
    }

    @Test
    fun failedTitleSaveKeepsEditorAndDraftOpen() {
        val state = DetailUiState(
            isEditingTitle = true,
            editedTitle = "Draft title",
            saveState = DetailSaveState.Saving(DetailEditCompletion.Title),
        )

        val actual = DetailReducer.reduce(
            state,
            DetailMutation.SaveFailed(
                completion = DetailEditCompletion.Title,
                errorCode = "CONFLICT",
            ),
        )

        assertTrue(actual.isEditingTitle)
        assertEquals("Draft title", actual.editedTitle)
        assertEquals(
            DetailSaveState.Failed(DetailEditCompletion.Title, "CONFLICT"),
            actual.saveState,
        )
    }

    @Test
    fun confirmedTitleSaveClosesEditor() {
        val state = DetailUiState(
            isEditingTitle = true,
            editedTitle = "Draft title",
            saveState = DetailSaveState.Saving(DetailEditCompletion.Title),
        )

        val actual = DetailReducer.reduce(
            state,
            DetailMutation.SaveSucceeded(DetailEditCompletion.Title),
        )

        assertFalse(actual.isEditingTitle)
        assertEquals(DetailSaveState.Idle, actual.saveState)
    }

    @Test
    fun confirmedNotesSaveDoesNotCloseTitleEditor() {
        val state = DetailUiState(
            isEditingTitle = true,
            editedTitle = "Draft title",
            saveState = DetailSaveState.Saving(DetailEditCompletion.Notes),
        )

        val actual = DetailReducer.reduce(
            state,
            DetailMutation.SaveSucceeded(DetailEditCompletion.Notes),
        )

        assertTrue(actual.isEditingTitle)
        assertEquals("Draft title", actual.editedTitle)
    }

    @Test
    fun successfulNotesAndDomainSavesCloseOnlyTheirOwnedDrafts() {
        val edits = DetailFieldEditState()
            .start(DetailEditKey.NOTES, "draft notes")
            .start(DetailEditKey.DOMAIN, "example.com")
        val notesSaved = DetailReducer.reduce(
            DetailUiState(
                fieldEdits = edits,
                saveState = DetailSaveState.Saving(DetailEditCompletion.Notes),
            ),
            DetailMutation.SaveSucceeded(DetailEditCompletion.Notes),
        )
        assertFalse(notesSaved.fieldEdits.isEditing(DetailEditKey.NOTES))
        assertTrue(notesSaved.fieldEdits.isEditing(DetailEditKey.DOMAIN))

        val domainSaved = DetailReducer.reduce(
            DetailUiState(
                fieldEdits = edits,
                saveState = DetailSaveState.Saving(DetailEditCompletion.Associations),
            ),
            DetailMutation.SaveSucceeded(DetailEditCompletion.Associations),
        )
        assertTrue(domainSaved.fieldEdits.isEditing(DetailEditKey.NOTES))
        assertFalse(domainSaved.fieldEdits.isEditing(DetailEditKey.DOMAIN))
    }

    @Test
    fun failedNotesSavePreservesViewModelOwnedDraft() {
        val state = DetailUiState(
            fieldEdits = DetailFieldEditState().start(DetailEditKey.NOTES, "draft notes"),
            saveState = DetailSaveState.Saving(DetailEditCompletion.Notes),
        )

        val actual = DetailReducer.reduce(
            state,
            DetailMutation.SaveFailed(DetailEditCompletion.Notes, "CONFLICT"),
        )

        assertTrue(actual.fieldEdits.isEditing(DetailEditKey.NOTES))
        assertEquals("draft notes", actual.fieldEdits.draft(DetailEditKey.NOTES))
        assertEquals(
            DetailSaveState.Failed(DetailEditCompletion.Notes, "CONFLICT"),
            actual.saveState,
        )
    }
}
