package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.entry.model.FieldKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailSaveStateTest {
    @Test
    fun repeatedInputWithTheSameValueDoesNotCreateANewDraft() {
        val title =
            DetailUiState(
                fieldEdits = DetailFieldEditState().start(DetailDraftKey.Title, "submitted"),
                saveState = DetailSaveState.Saving(DetailEditCompletion.Title),
            )
        val repeatedTitle = DetailReducer.reduce(title, DetailMutation.FieldDraftChanged(DetailDraftKey.Title, "submitted"))
        assertEquals(title, repeatedTitle)
        val notes =
            DetailUiState(
                fieldEdits = DetailFieldEditState().start(DetailDraftKey.Notes, "submitted"),
                saveState = DetailSaveState.Saving(DetailEditCompletion.Notes),
            )
        val repeatedNotes = DetailReducer.reduce(notes, DetailMutation.FieldDraftChanged(DetailDraftKey.Notes, "submitted"))
        assertEquals(notes, repeatedNotes)
        val saved =
            DetailReducer.reduce(
                repeatedNotes,
                DetailMutation.SaveSucceeded.DraftSaved(DetailEditCompletion.Notes, "submitted"),
            )
        assertFalse(saved.fieldEdits.edit(DetailDraftKey.Notes) is DetailFieldEdit.Editing)
    }

    @Test
    fun modifiedDraftSurvivesFailureAndANewSubmissionCanFinishIt() {
        val saving =
            DetailUiState(
                fieldEdits = DetailFieldEditState().start(DetailDraftKey.Notes, "submitted"),
                saveState = DetailSaveState.Saving(DetailEditCompletion.Notes),
            )
        val changed = DetailReducer.reduce(saving, DetailMutation.FieldDraftChanged(DetailDraftKey.Notes, "new draft"))
        val failed = DetailReducer.reduce(changed, DetailMutation.SaveFailed(DetailEditCompletion.Notes))
        assertEquals(DetailSaveState.Idle, failed.saveState)
        assertEquals(
            DetailFieldEdit.Editing(initialValue = "submitted", draft = "new draft"),
            failed.fieldEdits.edit(DetailDraftKey.Notes),
        )
        val retry = DetailReducer.reduce(failed, DetailMutation.SaveStarted(DetailEditCompletion.Notes))
        assertEquals(DetailSaveState.Saving(DetailEditCompletion.Notes), retry.saveState)
        val saved =
            DetailReducer.reduce(
                retry,
                DetailMutation.SaveSucceeded.DraftSaved(DetailEditCompletion.Notes, "submitted"),
            )
        assertFalse(saved.fieldEdits.edit(DetailDraftKey.Notes) is DetailFieldEdit.Editing)
    }

    @Test
    fun unrelatedDraftChangesDoNotKeepTheSubmittedDraftOpen() {
        val saving =
            DetailUiState(
                fieldEdits = DetailFieldEditState().start(DetailDraftKey.Notes, "submitted").start(DetailDraftKey.Domain, "old"),
                saveState = DetailSaveState.Saving(DetailEditCompletion.Notes),
            )
        val changed = DetailReducer.reduce(saving, DetailMutation.FieldDraftChanged(DetailDraftKey.Domain, "new.example"))
        val saved =
            DetailReducer.reduce(
                changed,
                DetailMutation.SaveSucceeded.DraftSaved(DetailEditCompletion.Notes, "submitted"),
            )
        assertFalse(saved.fieldEdits.edit(DetailDraftKey.Notes) is DetailFieldEdit.Editing)
        assertEquals(
            DetailFieldEdit.Editing(initialValue = "old", draft = "new.example"),
            saved.fieldEdits.edit(DetailDraftKey.Domain),
        )
    }

    @Test
    fun cancelledAndReopenedTitleIsANewDraftEvenWithTheSameText() {
        val saving =
            DetailUiState(
                fieldEdits = DetailFieldEditState().start(DetailDraftKey.Title, "submitted"),
                saveState = DetailSaveState.Saving(DetailEditCompletion.Title),
            )
        val cancelled = DetailReducer.reduce(saving, DetailMutation.FieldEditingCancelled(DetailDraftKey.Title))
        val reopened = DetailReducer.reduce(cancelled, DetailMutation.FieldEditingStarted(DetailDraftKey.Title, "submitted"))
        val changed = DetailReducer.reduce(reopened, DetailMutation.FieldDraftChanged(DetailDraftKey.Title, "submitted"))
        val saved =
            DetailReducer.reduce(
                changed,
                DetailMutation.SaveSucceeded.DraftSaved(DetailEditCompletion.Title, "submitted"),
            )
        assertEquals(
            DetailFieldEdit.Editing(initialValue = "submitted", draft = "submitted"),
            saved.fieldEdits.edit(DetailDraftKey.Title),
        )
    }

    @Test
    fun titleChangedDuringSaveRemainsOpenAfterTheEarlierWriteCompletes() {
        val saving =
            DetailUiState(
                fieldEdits = DetailFieldEditState().start(DetailDraftKey.Title, "submitted"),
                saveState = DetailSaveState.Saving(DetailEditCompletion.Title),
            )
        val changed = DetailReducer.reduce(saving, DetailMutation.FieldDraftChanged(DetailDraftKey.Title, "new draft"))
        val saved =
            DetailReducer.reduce(
                changed,
                DetailMutation.SaveSucceeded.DraftSaved(DetailEditCompletion.Title, "submitted"),
            )
        assertEquals(
            DetailFieldEdit.Editing(initialValue = "submitted", draft = "new draft"),
            saved.fieldEdits.edit(DetailDraftKey.Title),
        )
        assertEquals(DetailSaveState.Idle, saved.saveState)
    }

    @Test
    fun textDraftsChangedOrReopenedDuringSaveSurviveTheEarlierCompletion() {
        val cases: List<Triple<DetailDraftKey, DetailEditCompletion.Draft, String>> =
            listOf(
                Triple(DetailDraftKey.Notes, DetailEditCompletion.Notes, "saved notes"),
                Triple(DetailDraftKey.Domain, DetailEditCompletion.Domain, "saved.example"),
                Triple(
                    DetailDraftKey.EntryField(FieldKey.PASSWORD),
                    DetailEditCompletion.Field(FieldKey.PASSWORD),
                    "saved password",
                ),
            )
        for ((key, completion, persistedValue) in cases) {
            val saving =
                DetailUiState(
                    fieldEdits = DetailFieldEditState().start(key, "submitted"),
                    saveState = DetailSaveState.Saving(completion),
                )
            val changed = DetailReducer.reduce(saving, DetailMutation.FieldDraftChanged(key, "new draft"))
            val saved =
                DetailReducer.reduce(
                    changed,
                    DetailMutation.SaveSucceeded.DraftSaved(completion, persistedValue),
                )
            assertEquals(
                DetailFieldEdit.Editing(initialValue = persistedValue, draft = "new draft"),
                saved.fieldEdits.edit(key),
            )
            val cancelled = DetailReducer.reduce(saving, DetailMutation.FieldEditingCancelled(key))
            val reopened = DetailReducer.reduce(cancelled, DetailMutation.FieldEditingStarted(key, "submitted"))
            val completed =
                DetailReducer.reduce(
                    reopened,
                    DetailMutation.SaveSucceeded.DraftSaved(completion, persistedValue),
                )
            assertEquals(
                DetailFieldEdit.Editing(initialValue = persistedValue, draft = "submitted"),
                completed.fieldEdits.edit(key),
            )
        }
    }

    @Test
    fun changedFieldDraftRebasesToTheValueCommittedByAnEarlierSave() {
        val key = DetailDraftKey.EntryField(FieldKey.PASSWORD)
        val editing = DetailFieldEditState().start(key, "A")
        val completion = DetailEditCompletion.Field(FieldKey.PASSWORD)
        val edited =
            DetailReducer.reduce(
                DetailUiState(fieldEdits = editing),
                DetailMutation.FieldDraftChanged(key, "B"),
            )
        val saving = DetailReducer.reduce(edited, DetailMutation.SaveStarted(completion))
        val changedWhileSaving = DetailReducer.reduce(saving, DetailMutation.FieldDraftChanged(key, "A"))

        val saved =
            DetailReducer.reduce(
                changedWhileSaving,
                DetailMutation.SaveSucceeded.DraftSaved(completion, "B"),
            )

        assertEquals(
            DetailFieldEdit.Editing(initialValue = "B", draft = "A"),
            saved.fieldEdits.edit(key),
        )
    }

    @Test
    fun savingApplicationsDoesNotCloseTheDomainDraft() {
        val saving =
            DetailUiState(
                fieldEdits = DetailFieldEditState().start(DetailDraftKey.Domain, "unsaved.example"),
                saveState = DetailSaveState.Saving(DetailEditCompletion.Applications),
            )
        val saved = DetailReducer.reduce(saving, DetailMutation.SaveSucceeded.OtherSaved(DetailEditCompletion.Applications))
        assertEquals(
            DetailFieldEdit.Editing(initialValue = "unsaved.example", draft = "unsaved.example"),
            saved.fieldEdits.edit(DetailDraftKey.Domain),
        )
    }

    @Test
    fun saveStartedTracksOnlyTheRequestedEdit() {
        val actual =
            DetailReducer.reduce(
                DetailUiState(),
                DetailMutation.SaveStarted(DetailEditCompletion.Notes),
            )

        assertEquals(
            DetailSaveState.Saving(DetailEditCompletion.Notes),
            actual.saveState,
        )
    }

    @Test
    fun failedTitleSaveKeepsEditorAndDraftOpen() {
        val state =
            DetailUiState(
                fieldEdits = DetailFieldEditState().start(DetailDraftKey.Title, "Draft title"),
                saveState = DetailSaveState.Saving(DetailEditCompletion.Title),
            )

        val actual =
            DetailReducer.reduce(
                state,
                DetailMutation.SaveFailed(
                    completion = DetailEditCompletion.Title,
                ),
            )

        assertEquals(
            DetailFieldEdit.Editing(initialValue = "Draft title", draft = "Draft title"),
            actual.fieldEdits.edit(DetailDraftKey.Title),
        )
        assertEquals(
            DetailSaveState.Idle,
            actual.saveState,
        )
    }

    @Test
    fun confirmedTitleSaveClosesEditor() {
        val state =
            DetailUiState(
                fieldEdits = DetailFieldEditState().start(DetailDraftKey.Title, "Draft title"),
                saveState = DetailSaveState.Saving(DetailEditCompletion.Title),
            )

        val actual =
            DetailReducer.reduce(
                state,
                DetailMutation.SaveSucceeded.DraftSaved(DetailEditCompletion.Title, "Draft title"),
            )

        assertFalse(actual.fieldEdits.edit(DetailDraftKey.Title) is DetailFieldEdit.Editing)
        assertEquals(DetailSaveState.Idle, actual.saveState)
    }

    @Test
    fun confirmedNotesSaveDoesNotCloseTitleEditor() {
        val state =
            DetailUiState(
                fieldEdits = DetailFieldEditState().start(DetailDraftKey.Title, "Draft title"),
                saveState = DetailSaveState.Saving(DetailEditCompletion.Notes),
            )

        val actual =
            DetailReducer.reduce(
                state,
                DetailMutation.SaveSucceeded.DraftSaved(DetailEditCompletion.Notes, "Draft notes"),
            )

        assertEquals(
            DetailFieldEdit.Editing(initialValue = "Draft title", draft = "Draft title"),
            actual.fieldEdits.edit(DetailDraftKey.Title),
        )
    }

    @Test
    fun successfulNotesAndDomainSavesCloseOnlyTheirOwnedDrafts() {
        val edits =
            DetailFieldEditState()
                .start(DetailDraftKey.Notes, "draft notes")
                .start(DetailDraftKey.Domain, "example.com")
        val notesSaved =
            DetailReducer.reduce(
                DetailUiState(
                    fieldEdits = edits,
                    saveState = DetailSaveState.Saving(DetailEditCompletion.Notes),
                ),
                DetailMutation.SaveSucceeded.DraftSaved(DetailEditCompletion.Notes, "draft notes"),
            )
        assertFalse(notesSaved.fieldEdits.edit(DetailDraftKey.Notes) is DetailFieldEdit.Editing)
        assertTrue(notesSaved.fieldEdits.edit(DetailDraftKey.Domain) is DetailFieldEdit.Editing)

        val domainSaved =
            DetailReducer.reduce(
                DetailUiState(
                    fieldEdits = edits,
                    saveState = DetailSaveState.Saving(DetailEditCompletion.Domain),
                ),
                DetailMutation.SaveSucceeded.DraftSaved(DetailEditCompletion.Domain, "example.com"),
            )
        assertTrue(domainSaved.fieldEdits.edit(DetailDraftKey.Notes) is DetailFieldEdit.Editing)
        assertFalse(domainSaved.fieldEdits.edit(DetailDraftKey.Domain) is DetailFieldEdit.Editing)
    }

    @Test
    fun failedNotesSavePreservesViewModelOwnedDraft() {
        val state =
            DetailUiState(
                fieldEdits = DetailFieldEditState().start(DetailDraftKey.Notes, "draft notes"),
                saveState = DetailSaveState.Saving(DetailEditCompletion.Notes),
            )

        val actual =
            DetailReducer.reduce(
                state,
                DetailMutation.SaveFailed(DetailEditCompletion.Notes),
            )

        assertEquals(
            DetailFieldEdit.Editing(initialValue = "draft notes", draft = "draft notes"),
            actual.fieldEdits.edit(DetailDraftKey.Notes),
        )
        assertEquals(
            DetailSaveState.Idle,
            actual.saveState,
        )
    }
}
