package com.aozijx.passly.feature.vault.presentation.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailFaviconReducerTest {

    @Test
    fun openMapsBuiltInIconToSingleDraftSource() {
        val actual = DetailReducer.reduce(
            DetailUiState(),
            DetailMutation.FaviconEditorOpened(
                DetailFaviconSource.BuiltIn("security.key", "primary"),
            ),
        )

        val editor = actual.faviconEditor as DetailFaviconEditorState.Open
        assertEquals(
            DetailFaviconSource.BuiltIn("security.key", "primary"),
            editor.source,
        )
        assertFalse(editor.dirty)
    }

    @Test
    fun selectingDefaultMakesBuiltInDraftDirty() {
        val opened = DetailFaviconEditorState.Open(
            initialSource = DetailFaviconSource.BuiltIn("security.key", null),
        )

        val actual = DetailFaviconEditorReducer.reduce(
            opened,
            DetailMutation.FaviconSourceChanged(DetailFaviconSource.InferredDefault),
        ) as DetailFaviconEditorState.Open

        assertEquals(DetailFaviconSource.InferredDefault, actual.source)
        assertTrue(actual.dirty)
    }

    @Test
    fun dirtyDismissConfirmsAndFailurePreservesDraftWhileSuccessCloses() {
        val editor = DetailFaviconEditorState.Open(
            initialSource = DetailFaviconSource.InferredDefault,
            source = DetailFaviconSource.BuiltIn("finance.bank", "secondary"),
        )
        val requested = DetailFaviconEditorReducer.reduce(
            editor,
            DetailMutation.FaviconEditorDismissRequested,
        ) as DetailFaviconEditorState.Open
        val saving = DetailUiState(
            saveState = DetailSaveState.Saving(DetailEditCompletion.Icon),
            faviconEditor = requested,
        )

        val failed = DetailReducer.reduce(
            saving,
            DetailMutation.SaveFailed(DetailEditCompletion.Icon, "CONFLICT"),
        )
        val succeeded = DetailReducer.reduce(
            saving,
            DetailMutation.SaveSucceeded(DetailEditCompletion.Icon),
        )

        val failedEditor = failed.faviconEditor as DetailFaviconEditorState.Open
        assertEquals(DetailFaviconOverlay.ConfirmDiscard, requested.overlay)
        assertEquals(editor.source, failedEditor.source)
        assertEquals(
            DetailFaviconWorkState.Failed(DetailFaviconProcessingError.SAVE_FAILED),
            failedEditor.work,
        )
        assertEquals(DetailFaviconEditorState.Closed, succeeded.faviconEditor)
    }

    @Test
    fun processingFailurePreservesPendingCropInput() {
        val state = DetailFaviconEditorState.Open(
            initialSource = DetailFaviconSource.InferredDefault,
            work = DetailFaviconWorkState.Processing,
            pendingInputPath = "/private/staging/input",
        )

        val actual = DetailFaviconEditorReducer.reduce(
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
    fun iconSaveStartClearsPreviousEditorError() {
        val state = DetailUiState(
            faviconEditor = DetailFaviconEditorState.Open(
                initialSource = DetailFaviconSource.InferredDefault,
                work = DetailFaviconWorkState.Failed(
                    DetailFaviconProcessingError.SAVE_FAILED,
                ),
            ),
        )

        val actual = DetailReducer.reduce(
            state,
            DetailMutation.SaveStarted(DetailEditCompletion.Icon),
        )

        assertEquals(
            DetailSaveState.Saving(DetailEditCompletion.Icon),
            actual.saveState,
        )
        assertEquals(
            DetailFaviconWorkState.Idle,
            (actual.faviconEditor as DetailFaviconEditorState.Open).work,
        )
    }

    @Test
    fun processedPrivateImageEndsProcessingSoSaveCanRun() {
        val state = DetailFaviconEditorState.Open(
            work = DetailFaviconWorkState.Processing,
            initialSource = DetailFaviconSource.InferredDefault,
            source = DetailFaviconSource.InferredDefault,
        )

        val actual = DetailFaviconEditorReducer.reduce(
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
        val actual = DetailFaviconEditorReducer.reduce(
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
    fun promotedPrivateImageKeepsCandidatePathUntilSaveCompletes() {
        val state = DetailFaviconEditorState.Open(
            initialSource = DetailFaviconSource.InferredDefault,
            source = DetailFaviconSource.PrivateImage("/private/staging/icon.webp"),
        )

        val promoted = DetailFaviconEditorReducer.reduce(
            state,
            DetailMutation.FaviconSourcePromoted("/private/images/favicon.webp"),
        ) as DetailFaviconEditorState.Open

        assertEquals(
            DetailFaviconSource.PrivateImage("/private/images/favicon.webp"),
            promoted.source,
        )
        assertEquals(
            "/private/images/favicon.webp",
            promoted.promotedCandidatePath,
        )

        val saved = DetailReducer.reduce(
            DetailUiState(
                saveState = DetailSaveState.Saving(DetailEditCompletion.Icon),
                faviconEditor = promoted,
            ),
            DetailMutation.SaveSucceeded(DetailEditCompletion.Icon),
        )

        assertEquals(DetailFaviconEditorState.Closed, saved.faviconEditor)
    }

    @Test
    fun keepEditingOnlyClearsDiscardConfirmation() {
        val state = DetailFaviconEditorState.Open(
            initialSource = DetailFaviconSource.InferredDefault,
            overlay = DetailFaviconOverlay.ConfirmDiscard,
        )

        val actual = DetailFaviconEditorReducer.reduce(
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
