package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.entry.policy.EntryTagValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailTagEditorReducerTest {
    @Test
    fun `closed editor ignores mutations until it is opened`() {
        assertEquals(
            DetailTagEditorState.Closed,
            DetailTagEditorReducer.reduce(
                DetailTagEditorState.Closed,
                DetailMutation.TagInputChanged("ignored"),
            ),
        )
    }

    @Test
    fun inputChangeFiltersSuggestionsIgnoringSelectedTags() {
        val opened =
            DetailTagEditorState.Open(
                initialTags = setOf("Passkey"),
                availableTags = setOf("Passkey", "Password", "Personal"),
            )

        val actual =
            DetailTagEditorReducer.reduce(
                opened,
                DetailMutation.TagInputChanged("pas"),
            ) as DetailTagEditorState.Open

        assertEquals("pas", actual.input)
        assertEquals(listOf("Password"), actual.suggestions)
    }

    @Test
    fun addingAndRemovingTagsUpdatesDirtyDraft() {
        val opened =
            DetailTagEditorState.Open(
                initialTags = linkedSetOf("Work"),
            )

        val added =
            DetailTagEditorReducer.reduce(
                opened,
                DetailMutation.TagSubmitted("Personal"),
            )
        val removed =
            DetailTagEditorReducer.reduce(
                added,
                DetailMutation.TagRemoved("work"),
            ) as DetailTagEditorState.Open

        assertEquals(linkedSetOf("Personal"), removed.draftTags)
        assertTrue(removed.dirty)
        assertEquals(DetailTagValidationState.None, removed.validation)
    }

    @Test
    fun invalidTagPreservesInputAndDraft() {
        val opened =
            DetailTagEditorState.Open(
                initialTags = emptySet(),
                input = "x".repeat(33),
            )

        val actual =
            DetailTagEditorReducer.reduce(
                opened,
                DetailMutation.TagSubmitted(opened.input),
            ) as DetailTagEditorState.Open

        assertEquals(emptySet<String>(), actual.draftTags)
        assertEquals("x".repeat(33), actual.input)
        assertEquals(
            DetailTagValidationState.Invalid(EntryTagValidationError.TooLong(maximum = 32)),
            actual.validation,
        )
    }

    @Test
    fun dirtyDismissRequestsConfirmationAndConfirmedDiscardClosesSheet() {
        val dirty =
            DetailTagEditorState.Open(
                initialTags = setOf("Work"),
                draftTags = setOf("Personal"),
            )

        val requested =
            DetailTagEditorReducer.reduce(
                dirty,
                DetailMutation.TagEditorDismissRequested,
            ) as DetailTagEditorState.Open
        val discarded = DetailTagEditorReducer.reduce(requested, DetailMutation.TagEditorDiscardConfirmed)

        assertTrue(requested.showDiscardConfirmation)
        assertEquals(DetailTagEditorState.Closed, discarded)
    }

}
