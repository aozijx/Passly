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

        assertTrue(actual.tagEditor.visible)
        assertEquals(linkedSetOf("Personal", "Work"), actual.tagEditor.initialTags)
        assertEquals(linkedSetOf("Personal", "Work"), actual.tagEditor.draftTags)
        assertEquals(setOf("Password", "Finance"), actual.tagEditor.availableTags)
        assertFalse(actual.tagEditor.dirty)
    }

    @Test
    fun inputChangeFiltersSuggestionsIgnoringSelectedTags() {
        val opened = DetailTagEditorState(
            visible = true,
            initialTags = setOf("Passkey"),
            draftTags = setOf("Passkey"),
            availableTags = setOf("Passkey", "Password", "Personal"),
        )

        val actual = DetailTagEditorReducer.reduce(opened, DetailMutation.TagInputChanged("pas"))

        assertEquals("pas", actual.input)
        assertEquals(listOf("Password"), actual.suggestions)
    }

    @Test
    fun addingAndRemovingTagsUpdatesDirtyDraft() {
        val opened = DetailTagEditorState(
            visible = true,
            initialTags = linkedSetOf("Work"),
            draftTags = linkedSetOf("Work"),
        )

        val added = DetailTagEditorReducer.reduce(opened, DetailMutation.TagSubmitted("Personal"))
        val removed = DetailTagEditorReducer.reduce(added, DetailMutation.TagRemoved("work"))

        assertEquals(linkedSetOf("Personal"), removed.draftTags)
        assertTrue(removed.dirty)
        assertNull(removed.validationError)
    }

    @Test
    fun invalidTagPreservesInputAndDraft() {
        val opened = DetailTagEditorState(
            visible = true,
            initialTags = emptySet(),
            draftTags = emptySet(),
            input = "x".repeat(33),
        )

        val actual = DetailTagEditorReducer.reduce(
            opened,
            DetailMutation.TagSubmitted(opened.input),
        )

        assertEquals(emptySet<String>(), actual.draftTags)
        assertEquals("x".repeat(33), actual.input)
        assertEquals(
            DetailTagValidationError.TAG_TOO_LONG,
            actual.validationError,
        )
    }

    @Test
    fun dirtyDismissRequestsConfirmationAndConfirmedDiscardClosesSheet() {
        val dirty = DetailTagEditorState(
            visible = true,
            initialTags = setOf("Work"),
            draftTags = setOf("Personal"),
        )

        val requested = DetailTagEditorReducer.reduce(dirty, DetailMutation.TagEditorDismissRequested)
        val discarded = DetailTagEditorReducer.reduce(requested, DetailMutation.TagEditorDiscardConfirmed)

        assertTrue(requested.visible)
        assertTrue(requested.confirmDiscard)
        assertEquals(DetailTagEditorState(), discarded)
    }

    @Test
    fun successfulTagSaveClosesSheetButFailurePreservesDraft() {
        val saving = DetailUiState(
            savingEdit = DetailEditCompletion.Tags,
            tagEditor = DetailTagEditorState(
                visible = true,
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

        assertTrue(failed.tagEditor.visible)
        assertEquals(setOf("Personal"), failed.tagEditor.draftTags)
        assertEquals(DetailTagEditorState(), succeeded.tagEditor)
    }
}
