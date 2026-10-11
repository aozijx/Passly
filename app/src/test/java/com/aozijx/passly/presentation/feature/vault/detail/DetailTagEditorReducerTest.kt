package com.aozijx.passly.presentation.feature.vault.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailTagEditorReducerTest {

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
