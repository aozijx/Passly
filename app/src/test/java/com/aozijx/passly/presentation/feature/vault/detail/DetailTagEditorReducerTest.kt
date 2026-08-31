package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.presentation.ui.vault.detail.model.DetailTagEditorUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.TagEditorValidationErrorUiModel
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
        val opened = DetailUiState(
            tagEditor = DetailTagEditorUiModel(
                visible = true,
                initialTags = setOf("Passkey"),
                draftTags = setOf("Passkey"),
                availableTags = setOf("Passkey", "Password", "Personal"),
            ),
        )

        val actual = DetailReducer.reduce(opened, DetailMutation.TagInputChanged("pas"))

        assertEquals("pas", actual.tagEditor.input)
        assertEquals(listOf("Password"), actual.tagEditor.suggestions)
    }

    @Test
    fun addingAndRemovingTagsUpdatesDirtyDraft() {
        val opened = DetailUiState(
            tagEditor = DetailTagEditorUiModel(
                visible = true,
                initialTags = linkedSetOf("Work"),
                draftTags = linkedSetOf("Work"),
            ),
        )

        val added = DetailReducer.reduce(opened, DetailMutation.TagSubmitted("Personal"))
        val removed = DetailReducer.reduce(added, DetailMutation.TagRemoved("work"))

        assertEquals(linkedSetOf("Personal"), removed.tagEditor.draftTags)
        assertTrue(removed.tagEditor.dirty)
        assertNull(removed.tagEditor.validationError)
    }

    @Test
    fun invalidTagPreservesInputAndDraft() {
        val opened = DetailUiState(
            tagEditor = DetailTagEditorUiModel(
                visible = true,
                initialTags = emptySet(),
                draftTags = emptySet(),
                input = "x".repeat(33),
            ),
        )

        val actual = DetailReducer.reduce(
            opened,
            DetailMutation.TagSubmitted(opened.tagEditor.input),
        )

        assertEquals(emptySet<String>(), actual.tagEditor.draftTags)
        assertEquals("x".repeat(33), actual.tagEditor.input)
        assertEquals(
            TagEditorValidationErrorUiModel.TAG_TOO_LONG,
            actual.tagEditor.validationError,
        )
    }

    @Test
    fun dirtyDismissRequestsConfirmationAndConfirmedDiscardClosesSheet() {
        val dirty = DetailUiState(
            tagEditor = DetailTagEditorUiModel(
                visible = true,
                initialTags = setOf("Work"),
                draftTags = setOf("Personal"),
            ),
        )

        val requested = DetailReducer.reduce(dirty, DetailMutation.TagEditorDismissRequested)
        val discarded = DetailReducer.reduce(requested, DetailMutation.TagEditorDiscardConfirmed)

        assertTrue(requested.tagEditor.visible)
        assertTrue(requested.tagEditor.confirmDiscard)
        assertEquals(DetailTagEditorUiModel(), discarded.tagEditor)
    }

    @Test
    fun successfulTagSaveClosesSheetButFailurePreservesDraft() {
        val saving = DetailUiState(
            savingEdit = DetailEditCompletion.Tags,
            tagEditor = DetailTagEditorUiModel(
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
        assertEquals(DetailTagEditorUiModel(), succeeded.tagEditor)
    }
}
