package com.aozijx.passly.presentation.feature.vault.detail

import org.junit.Assert.assertEquals
import org.junit.Test

class DetailFaviconStateModelTest {
    @Test
    fun `closed editor cannot carry processing or discard state`() {
        assertEquals(
            DetailFaviconEditorState.Closed,
            DetailFaviconEditorReducer.reduce(
                DetailFaviconEditorState.Closed,
                DetailMutation.FaviconProcessingStarted,
            ),
        )
    }

    @Test
    fun `processing failure and discard confirmation are explicit open states`() {
        val opened = DetailFaviconEditorReducer.reduce(
            DetailFaviconEditorState.Closed,
            DetailMutation.FaviconEditorOpened(DetailFaviconSource.InferredDefault),
        ) as DetailFaviconEditorState.Open
        val processing = DetailFaviconEditorReducer.reduce(
            opened,
            DetailMutation.FaviconProcessingStarted,
        ) as DetailFaviconEditorState.Open
        val failed = DetailFaviconEditorReducer.reduce(
            processing,
            DetailMutation.FaviconProcessingFailed(DetailFaviconProcessingError.INVALID_IMAGE),
        ) as DetailFaviconEditorState.Open
        val dirty = failed.copy(source = DetailFaviconSource.BuiltIn("finance.bank", null))
        val confirming = DetailFaviconEditorReducer.reduce(
            dirty,
            DetailMutation.FaviconEditorDismissRequested,
        ) as DetailFaviconEditorState.Open

        assertEquals(DetailFaviconWorkState.Processing, processing.work)
        assertEquals(
            DetailFaviconWorkState.Failed(DetailFaviconProcessingError.INVALID_IMAGE),
            failed.work,
        )
        assertEquals(DetailFaviconOverlay.ConfirmDiscard, confirming.overlay)
    }
}
