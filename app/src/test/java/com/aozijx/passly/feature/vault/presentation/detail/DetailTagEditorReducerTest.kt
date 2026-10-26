package com.aozijx.passly.feature.vault.presentation.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailTagEditorReducerTest {

    @Test
    fun savingTagsRejectsEditsAndDismissalsUntilTheWriteCompletes() {
        val editor = DetailTagEditorState.Open(initialTags = setOf("Work"), input = "Personal")
        val saving = DetailUiState(tagEditor = editor, saveState = DetailSaveState.Saving(DetailEditCompletion.Tags))
        val changes = listOf(
            DetailMutation.TagInputChanged("New draft"),
            DetailMutation.TagSubmitted("Finance"),
            DetailMutation.TagRemoved("Work"),
            DetailMutation.TagEditorDismissRequested,
            DetailMutation.TagEditorDiscardConfirmed,
            DetailMutation.TagEditorDiscardCancelled,
            DetailMutation.TagEditorOpened(setOf("Other"), emptySet()),
        )
        for (change in changes) assertEquals(saving, DetailReducer.reduce(saving, change))
        val failed = DetailReducer.reduce(saving, DetailMutation.SaveFailed(DetailEditCompletion.Tags, "failure"))
        val edited = DetailReducer.reduce(failed, DetailMutation.TagInputChanged("Try again"))
        assertEquals("Try again", (edited.tagEditor as DetailTagEditorState.Open).input)
        val succeeded = DetailReducer.reduce(saving, DetailMutation.SaveSucceeded(DetailEditCompletion.Tags))
        assertEquals(DetailTagEditorState.Closed, succeeded.tagEditor)
    }

    @Test
    fun savingAnUnrelatedFieldDoesNotBlockTheTagDraft() {
        val state = DetailUiState(
            tagEditor = DetailTagEditorState.Open(initialTags = emptySet()),
            saveState = DetailSaveState.Saving(DetailEditCompletion.Favorite),
        )
        val edited = DetailReducer.reduce(state, DetailMutation.TagInputChanged("Personal"))
        assertEquals("Personal", (edited.tagEditor as DetailTagEditorState.Open).input)
    }

    @Test
    fun openInitializesSortedDraftAndAvailableSuggestions() {
        val actual = DetailReducer.reduce(
            DetailUiState(),
            DetailMutation.TagEditorOpened(
                currentTags = setOf("Work", "Personal"),
                availableTags = setOf("Password", "Finance"),
            ),
        )

        val editor = actual.tagEditor as DetailTagEditorState.Open
        assertEquals(linkedSetOf("Personal", "Work"), editor.initialTags)
        assertEquals(linkedSetOf("Personal", "Work"), editor.draftTags)
        assertEquals(setOf("Password", "Finance"), editor.availableTags)
        assertFalse(editor.dirty)
    }

    @Test
    fun inputChangeFiltersSuggestionsIgnoringSelectedTags() {
        val opened = DetailTagEditorState.Open(
            initialTags = setOf("Passkey"),
            availableTags = setOf("Passkey", "Password", "Personal"),
        )

        val actual = DetailTagEditorReducer.reduce(
            opened,
            DetailMutation.TagInputChanged("pas"),
        ) as DetailTagEditorState.Open

        assertEquals("pas", actual.input)
        assertEquals(listOf("Password"), actual.suggestions)
    }

    @Test
    fun addingAndRemovingTagsUpdatesDirtyDraft() {
        val opened = DetailTagEditorState.Open(
            initialTags = linkedSetOf("Work"),
        )

        val added = DetailTagEditorReducer.reduce(
            opened,
            DetailMutation.TagSubmitted("Personal"),
        )
        val removed = DetailTagEditorReducer.reduce(
            added,
            DetailMutation.TagRemoved("work"),
        ) as DetailTagEditorState.Open

        assertEquals(linkedSetOf("Personal"), removed.draftTags)
        assertTrue(removed.dirty)
        assertNull(removed.validationError)
    }

    @Test
    fun invalidTagPreservesInputAndDraft() {
        val opened = DetailTagEditorState.Open(
            initialTags = emptySet(),
            input = "x".repeat(33),
        )

        val actual = DetailTagEditorReducer.reduce(
            opened,
            DetailMutation.TagSubmitted(opened.input),
        ) as DetailTagEditorState.Open

        assertEquals(emptySet<String>(), actual.draftTags)
        assertEquals("x".repeat(33), actual.input)
        assertEquals(
            DetailTagValidationError.TAG_TOO_LONG,
            actual.validationError,
        )
    }

    @Test
    fun dirtyDismissRequestsConfirmationAndConfirmedDiscardClosesSheet() {
        val dirty = DetailTagEditorState.Open(
            initialTags = setOf("Work"),
            draftTags = setOf("Personal"),
        )

        val requested = DetailTagEditorReducer.reduce(
            dirty,
            DetailMutation.TagEditorDismissRequested,
        ) as DetailTagEditorState.Open
        val discarded = DetailTagEditorReducer.reduce(requested, DetailMutation.TagEditorDiscardConfirmed)

        assertEquals(DetailTagEditorOverlay.ConfirmDiscard, requested.overlay)
        assertEquals(DetailTagEditorState.Closed, discarded)
    }

    @Test
    fun successfulTagSaveClosesSheetButFailurePreservesDraft() {
        val saving = DetailUiState(
            saveState = DetailSaveState.Saving(DetailEditCompletion.Tags),
            tagEditor = DetailTagEditorState.Open(
                initialTags = setOf("Work"),
                draftTags = setOf("Personal"),
            ),
        )

        val failed = DetailReducer.reduce(
            saving,
            DetailMutation.SaveFailed(DetailEditCompletion.Tags, "CONFLICT"),
        )
        val succeeded = DetailReducer.reduce(
            saving,
            DetailMutation.SaveSucceeded(DetailEditCompletion.Tags),
        )

        assertEquals(
            setOf("Personal"),
            (failed.tagEditor as DetailTagEditorState.Open).draftTags,
        )
        assertEquals(DetailTagEditorState.Closed, succeeded.tagEditor)
    }
}
