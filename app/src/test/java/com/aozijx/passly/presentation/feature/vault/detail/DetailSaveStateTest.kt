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
        assertEquals(0L, actual.saveCompletionId)
    }

    @Test
    fun confirmedTitleSaveClosesEditorAndPublishesCompletion() {
        val state = DetailUiState(
            isEditingTitle = true,
            editedTitle = "Draft title",
            savingEdit = DetailEditCompletion.Title,
            saveCompletionId = 4,
        )

        val actual = DetailReducer.reduce(
            state,
            DetailMutation.SaveSucceeded(DetailEditCompletion.Title),
        )

        assertFalse(actual.isEditingTitle)
        assertNull(actual.savingEdit)
        assertNull(actual.saveErrorCode)
        assertEquals(DetailEditCompletion.Title, actual.completedEdit)
        assertEquals(5L, actual.saveCompletionId)
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
        assertEquals(DetailEditCompletion.Notes, actual.completedEdit)
    }
}
