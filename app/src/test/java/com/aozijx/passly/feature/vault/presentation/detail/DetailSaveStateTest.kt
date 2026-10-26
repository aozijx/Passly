package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.entry.model.FieldKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailSaveStateTest {

    @Test
    fun repeatedInputWithTheSameValueDoesNotCreateANewDraft() {
        val title = DetailUiState(
            isEditingTitle = true, editedTitle = "submitted",
            saveState = DetailSaveState.Saving(DetailEditCompletion.Title),
        )
        val repeatedTitle = DetailReducer.reduce(title, DetailMutation.EditedTitleChanged("submitted"))
        assertEquals(title, repeatedTitle)
        val notes = DetailUiState(
            fieldEdits = DetailFieldEditState().start(DetailDraftKey.Notes, "submitted"),
            saveState = DetailSaveState.Saving(DetailEditCompletion.Notes),
        )
        val repeatedNotes = DetailReducer.reduce(notes, DetailMutation.FieldDraftChanged(DetailDraftKey.Notes, "submitted"))
        assertEquals(notes, repeatedNotes)
        val saved = DetailReducer.reduce(repeatedNotes, DetailMutation.SaveSucceeded(DetailEditCompletion.Notes))
        assertFalse(saved.fieldEdits.isEditing(DetailDraftKey.Notes))
    }

    @Test
    fun modifiedDraftSurvivesFailureAndANewSubmissionCanFinishIt() {
        val saving = DetailUiState(
            fieldEdits = DetailFieldEditState().start(DetailDraftKey.Notes, "submitted"),
            saveState = DetailSaveState.Saving(DetailEditCompletion.Notes),
        )
        val changed = DetailReducer.reduce(saving, DetailMutation.FieldDraftChanged(DetailDraftKey.Notes, "new draft"))
        val failed = DetailReducer.reduce(changed, DetailMutation.SaveFailed(DetailEditCompletion.Notes, "failure"))
        assertEquals(DetailSaveState.Failed(DetailEditCompletion.Notes, "failure"), failed.saveState)
        assertEquals("new draft", failed.fieldEdits.draft(DetailDraftKey.Notes))
        val retry = DetailReducer.reduce(failed, DetailMutation.SaveStarted(DetailEditCompletion.Notes))
        assertEquals(DetailSaveState.Saving(DetailEditCompletion.Notes), retry.saveState)
        val saved = DetailReducer.reduce(retry, DetailMutation.SaveSucceeded(DetailEditCompletion.Notes))
        assertFalse(saved.fieldEdits.isEditing(DetailDraftKey.Notes))
    }

    @Test
    fun unrelatedDraftChangesDoNotKeepTheSubmittedDraftOpen() {
        val saving = DetailUiState(
            fieldEdits = DetailFieldEditState().start(DetailDraftKey.Notes, "submitted").start(DetailDraftKey.Domain, "old"),
            saveState = DetailSaveState.Saving(DetailEditCompletion.Notes),
        )
        val changed = DetailReducer.reduce(saving, DetailMutation.FieldDraftChanged(DetailDraftKey.Domain, "new.example"))
        val saved = DetailReducer.reduce(changed, DetailMutation.SaveSucceeded(DetailEditCompletion.Notes))
        assertFalse(saved.fieldEdits.isEditing(DetailDraftKey.Notes))
        assertEquals("new.example", saved.fieldEdits.draft(DetailDraftKey.Domain))
    }

    @Test
    fun cancelledAndReopenedTitleIsANewDraftEvenWithTheSameText() {
        val saving = DetailUiState(
            isEditingTitle = true, editedTitle = "submitted",
            saveState = DetailSaveState.Saving(DetailEditCompletion.Title),
        )
        val cancelled = DetailReducer.reduce(saving, DetailMutation.TitleEditingCancelled)
        val reopened = DetailReducer.reduce(cancelled, DetailMutation.TitleEditingStarted)
        val changed = DetailReducer.reduce(reopened, DetailMutation.EditedTitleChanged("submitted"))
        val saved = DetailReducer.reduce(changed, DetailMutation.SaveSucceeded(DetailEditCompletion.Title))
        assertTrue(saved.isEditingTitle)
        assertEquals("submitted", saved.editedTitle)
    }

    @Test
    fun titleChangedDuringSaveRemainsOpenAfterTheEarlierWriteCompletes() {
        val saving = DetailUiState(
            isEditingTitle = true, editedTitle = "submitted",
            saveState = DetailSaveState.Saving(DetailEditCompletion.Title),
        )
        val changed = DetailReducer.reduce(saving, DetailMutation.EditedTitleChanged("new draft"))
        val saved = DetailReducer.reduce(changed, DetailMutation.SaveSucceeded(DetailEditCompletion.Title))
        assertTrue(saved.isEditingTitle)
        assertEquals("new draft", saved.editedTitle)
        assertEquals(DetailSaveState.Idle, saved.saveState)
    }

    @Test
    fun textDraftsChangedOrReopenedDuringSaveSurviveTheEarlierCompletion() {
        val cases = listOf(
            DetailDraftKey.Notes to DetailEditCompletion.Notes,
            DetailDraftKey.Domain to DetailEditCompletion.Domain,
            DetailDraftKey.EntryField(FieldKey.PASSWORD) to DetailEditCompletion.Field(FieldKey.PASSWORD),
        )
        for ((key, completion) in cases) {
            val saving = DetailUiState(
                fieldEdits = DetailFieldEditState().start(key, "submitted"),
                saveState = DetailSaveState.Saving(completion),
            )
            val changed = DetailReducer.reduce(saving, DetailMutation.FieldDraftChanged(key, "new draft"))
            val saved = DetailReducer.reduce(changed, DetailMutation.SaveSucceeded(completion))
            assertEquals("new draft", saved.fieldEdits.draft(key))
            assertTrue(saved.fieldEdits.isEditing(key))
            val cancelled = DetailReducer.reduce(saving, DetailMutation.FieldEditingCancelled(key))
            val reopened = DetailReducer.reduce(cancelled, DetailMutation.FieldEditingStarted(key, "submitted"))
            val completed = DetailReducer.reduce(reopened, DetailMutation.SaveSucceeded(completion))
            assertTrue(completed.fieldEdits.isEditing(key))
        }
    }

    @Test
    fun savingApplicationsDoesNotCloseTheDomainDraft() {
        val saving = DetailUiState(
            fieldEdits = DetailFieldEditState().start(DetailDraftKey.Domain, "unsaved.example"),
            saveState = DetailSaveState.Saving(DetailEditCompletion.Applications),
        )
        val saved = DetailReducer.reduce(saving, DetailMutation.SaveSucceeded(DetailEditCompletion.Applications))
        assertEquals("unsaved.example", saved.fieldEdits.draft(DetailDraftKey.Domain))
        assertTrue(saved.fieldEdits.isEditing(DetailDraftKey.Domain))
    }

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
            .start(DetailDraftKey.Notes, "draft notes")
            .start(DetailDraftKey.Domain, "example.com")
        val notesSaved = DetailReducer.reduce(
            DetailUiState(
                fieldEdits = edits,
                saveState = DetailSaveState.Saving(DetailEditCompletion.Notes),
            ),
            DetailMutation.SaveSucceeded(DetailEditCompletion.Notes),
        )
        assertFalse(notesSaved.fieldEdits.isEditing(DetailDraftKey.Notes))
        assertTrue(notesSaved.fieldEdits.isEditing(DetailDraftKey.Domain))

        val domainSaved = DetailReducer.reduce(
            DetailUiState(
                fieldEdits = edits,
                saveState = DetailSaveState.Saving(DetailEditCompletion.Domain),
            ),
            DetailMutation.SaveSucceeded(DetailEditCompletion.Domain),
        )
        assertTrue(domainSaved.fieldEdits.isEditing(DetailDraftKey.Notes))
        assertFalse(domainSaved.fieldEdits.isEditing(DetailDraftKey.Domain))
    }

    @Test
    fun failedNotesSavePreservesViewModelOwnedDraft() {
        val state = DetailUiState(
            fieldEdits = DetailFieldEditState().start(DetailDraftKey.Notes, "draft notes"),
            saveState = DetailSaveState.Saving(DetailEditCompletion.Notes),
        )

        val actual = DetailReducer.reduce(
            state,
            DetailMutation.SaveFailed(DetailEditCompletion.Notes, "CONFLICT"),
        )

        assertTrue(actual.fieldEdits.isEditing(DetailDraftKey.Notes))
        assertEquals("draft notes", actual.fieldEdits.draft(DetailDraftKey.Notes))
        assertEquals(
            DetailSaveState.Failed(DetailEditCompletion.Notes, "CONFLICT"),
            actual.saveState,
        )
    }
}
