package com.aozijx.passly.feature.vault.presentation.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailFaviconReducerTest {
    @Test
    fun `closed editor ignores processing mutations`() {
        assertEquals(
            DetailFaviconEditorState.Closed,
            DetailFaviconEditorReducer.reduce(
                DetailFaviconEditorState.Closed,
                DetailMutation.FaviconProcessingStarted,
            ),
        )
    }

    @Test
    fun `selecting the same promoted image preserves candidate ownership`() {
        val source = DetailFaviconSource.PrivateImage("promoted")
        val editor =
            DetailFaviconEditorState.Open(
                initialSource = DetailFaviconSource.InferredDefault,
                source = source,
                promotedCandidatePath = "promoted",
            )
        val updated = DetailFaviconEditorReducer.reduce(editor, DetailMutation.FaviconSourceChanged(source))
        assertEquals("promoted", (updated as DetailFaviconEditorState.Open).promotedCandidatePath)
    }

    @Test
    fun `changing url does not mark a running image operation idle`() {
        val editor =
            DetailFaviconEditorState.Open(
                initialSource = DetailFaviconSource.InferredDefault,
                work = DetailFaviconWorkState.Processing,
            )
        val updated = DetailFaviconEditorReducer.reduce(editor, DetailMutation.FaviconImageUrlChanged("https://example.com/icon"))
        assertEquals(DetailFaviconWorkState.Processing, (updated as DetailFaviconEditorState.Open).work)
    }

    @Test
    fun selectingDefaultMakesBuiltInDraftDirty() {
        val opened =
            DetailFaviconEditorState.Open(
                initialSource = DetailFaviconSource.BuiltIn("security.key", null),
            )

        val actual =
            DetailFaviconEditorReducer.reduce(
                opened,
                DetailMutation.FaviconSourceChanged(DetailFaviconSource.InferredDefault),
            ) as DetailFaviconEditorState.Open

        assertEquals(DetailFaviconSource.InferredDefault, actual.source)
        assertTrue(actual.dirty)
    }

    @Test
    fun processingFailurePreservesPendingCropInput() {
        val state =
            DetailFaviconEditorState.Open(
                initialSource = DetailFaviconSource.InferredDefault,
                work = DetailFaviconWorkState.Processing,
                pendingInputPath = "/private/staging/input",
            )

        val actual =
            DetailFaviconEditorReducer.reduce(
                state,
                DetailMutation.FaviconProcessingFailed(DetailFaviconProcessingError.INVALID_IMAGE),
            ) as DetailFaviconEditorState.Open

        assertEquals("/private/staging/input", actual.pendingInputPath)
        assertEquals(
            DetailFaviconWorkState.Failed(DetailFaviconProcessingError.INVALID_IMAGE),
            actual.work,
        )
    }

    @Test
    fun processedPrivateImageEndsProcessingSoSaveCanRun() {
        val state =
            DetailFaviconEditorState.Open(
                work = DetailFaviconWorkState.Processing,
                initialSource = DetailFaviconSource.InferredDefault,
                source = DetailFaviconSource.InferredDefault,
            )

        val actual =
            DetailFaviconEditorReducer.reduce(
                state,
                DetailMutation.FaviconSourceChanged(
                    DetailFaviconSource.PrivateImage("/private/staging/icon.webp"),
                ),
            ) as DetailFaviconEditorState.Open

        assertEquals(DetailFaviconWorkState.Idle, actual.work)
        assertTrue(actual.dirty)
    }

    @Test
    fun openingPrivateImageStartsInCustomImageMode() {
        val actual =
            DetailFaviconEditorReducer.reduce(
                DetailFaviconEditorState.Closed,
                DetailMutation.FaviconEditorOpened(
                    DetailFaviconSource.PrivateImage("/private/images/favicon.webp"),
                ),
            )

        assertEquals(
            DetailFaviconTab.CUSTOM_IMAGE,
            (actual as DetailFaviconEditorState.Open).selectedTab,
        )
    }

    @Test
    fun keepEditingOnlyClearsDiscardConfirmation() {
        val state =
            DetailFaviconEditorState.Open(
                initialSource = DetailFaviconSource.InferredDefault,
                overlay = DetailFaviconOverlay.ConfirmDiscard,
            )

        val actual =
            DetailFaviconEditorReducer.reduce(
                state,
                DetailMutation.FaviconEditorDiscardCancelled,
            )

        assertEquals(state.copy(overlay = DetailFaviconOverlay.None), actual)
    }

    @Test
    fun editorOffersLibraryAndSingleCustomImageMode() {
        assertEquals(
            listOf("ICON_LIBRARY", "CUSTOM_IMAGE"),
            DetailFaviconTab.entries.map { it.name },
        )
    }
}
