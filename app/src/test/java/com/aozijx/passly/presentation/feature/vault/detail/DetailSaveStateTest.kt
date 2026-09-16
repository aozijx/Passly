package com.aozijx.passly.presentation.feature.vault.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailSaveStateTest {

    @Test
    fun saveStartedClearsOldErrorAndMarksOnlyRequestedEditAsSaving() {
        val state = DetailUiState(saveErrorCode = "OLD_ERROR")

        val actual = DetailReducer.reduce(
            state,
            DetailMutation.SaveStarted(DetailEditCompletion.Notes),
        )

        assertEquals(DetailEditCompletion.Notes, actual.savingEdit)
        assertNull(actual.saveErrorCode)
    }

    @Test
    fun failedTitleSaveKeepsEditorAndDraftOpen() {
        val state = DetailUiState(
            isEditingTitle = true,
            editedTitle = "Draft title",
            savingEdit = DetailEditCompletion.Title,
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
        assertNull(actual.savingEdit)
        assertEquals("CONFLICT", actual.saveErrorCode)
    }

    @Test
    fun confirmedTitleSaveClosesEditor() {
        val state = DetailUiState(
            isEditingTitle = true,
            editedTitle = "Draft title",
            savingEdit = DetailEditCompletion.Title,
        )

        val actual = DetailReducer.reduce(
            state,
            DetailMutation.SaveSucceeded(DetailEditCompletion.Title),
        )

        assertFalse(actual.isEditingTitle)
        assertNull(actual.savingEdit)
        assertNull(actual.saveErrorCode)
    }

    @Test
    fun confirmedNotesSaveDoesNotCloseTitleEditor() {
        val state = DetailUiState(
            isEditingTitle = true,
            editedTitle = "Draft title",
            savingEdit = DetailEditCompletion.Notes,
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
            DetailUiState(fieldEdits = edits, savingEdit = DetailEditCompletion.Notes),
            DetailMutation.SaveSucceeded(DetailEditCompletion.Notes),
        )
        assertFalse(notesSaved.fieldEdits.isEditing(DetailEditKey.NOTES))
        assertTrue(notesSaved.fieldEdits.isEditing(DetailEditKey.DOMAIN))

        val domainSaved = DetailReducer.reduce(
            DetailUiState(fieldEdits = edits, savingEdit = DetailEditCompletion.Associations),
            DetailMutation.SaveSucceeded(DetailEditCompletion.Associations),
        )
        assertTrue(domainSaved.fieldEdits.isEditing(DetailEditKey.NOTES))
        assertFalse(domainSaved.fieldEdits.isEditing(DetailEditKey.DOMAIN))
    }

    @Test
    fun failedNotesSavePreservesViewModelOwnedDraft() {
        val state = DetailUiState(
            fieldEdits = DetailFieldEditState().start(DetailEditKey.NOTES, "draft notes"),
            savingEdit = DetailEditCompletion.Notes,
        )

        val actual = DetailReducer.reduce(
            state,
            DetailMutation.SaveFailed(DetailEditCompletion.Notes, "CONFLICT"),
        )

        assertTrue(actual.fieldEdits.isEditing(DetailEditKey.NOTES))
        assertEquals("draft notes", actual.fieldEdits.draft(DetailEditKey.NOTES))
        assertEquals("CONFLICT", actual.saveErrorCode)
    }
}
