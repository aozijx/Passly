package com.aozijx.passly.feature.vault.presentation.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailTagEditorStateModelTest {
    @Test
    fun `closed editor ignores mutations until opened`() {
        assertEquals(
            DetailTagEditorState.Closed,
            DetailTagEditorReducer.reduce(
                DetailTagEditorState.Closed,
                DetailMutation.TagInputChanged("ignored"),
            ),
        )
    }

    @Test
    fun `dirty open editor expresses discard confirmation as an overlay`() {
        val opened = DetailTagEditorReducer.reduce(
            DetailTagEditorState.Closed,
            DetailMutation.TagEditorOpened(setOf("Personal"), setOf("Work")),
        ) as DetailTagEditorState.Open
        val dirty = DetailTagEditorReducer.reduce(
            opened,
            DetailMutation.TagInputChanged("New"),
        ) as DetailTagEditorState.Open
        val confirming = DetailTagEditorReducer.reduce(
            dirty,
            DetailMutation.TagEditorDismissRequested,
        ) as DetailTagEditorState.Open

        assertTrue(confirming.dirty)
        assertEquals(DetailTagEditorOverlay.ConfirmDiscard, confirming.overlay)
    }
}
