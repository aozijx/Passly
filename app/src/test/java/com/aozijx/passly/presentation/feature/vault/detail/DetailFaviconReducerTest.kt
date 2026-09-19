package com.aozijx.passly.presentation.feature.vault.detail

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

        assertTrue(actual.faviconEditor.visible)
        assertEquals(
            DetailFaviconSource.BuiltIn("security.key", "primary"),
            actual.faviconEditor.source,
        )
        assertFalse(actual.faviconEditor.dirty)
    }

    @Test
    fun selectingDefaultMakesBuiltInDraftDirty() {
        val opened = DetailFaviconEditorState(
            visible = true,
            initialSource = DetailFaviconSource.BuiltIn("security.key", null),
            source = DetailFaviconSource.BuiltIn("security.key", null),
        )

        val actual = DetailFaviconEditorReducer.reduce(
            opened,
            DetailMutation.FaviconSourceChanged(DetailFaviconSource.InferredDefault),
        )

        assertEquals(DetailFaviconSource.InferredDefault, actual.source)
        assertTrue(actual.dirty)
    }

    @Test
    fun dirtyDismissConfirmsAndFailurePreservesDraftWhileSuccessCloses() {
        val editor = DetailFaviconEditorState(
            visible = true,
            initialSource = DetailFaviconSource.InferredDefault,
            source = DetailFaviconSource.BuiltIn("finance.bank", "secondary"),
        )
        val requested = DetailFaviconEditorReducer.reduce(
            editor,
            DetailMutation.FaviconEditorDismissRequested,
        )
        val saving = DetailUiState(
            savingEdit = DetailEditCompletion.Icon,
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

        assertTrue(requested.confirmDiscard)
        assertEquals(editor.source, failed.faviconEditor.source)
        assertEquals(DetailFaviconProcessingError.SAVE_FAILED, failed.faviconEditor.processingError)
        assertEquals(DetailFaviconEditorState(), succeeded.faviconEditor)
    }

    @Test
    fun processingFailurePreservesPendingCropInput() {
        val state = DetailFaviconEditorState(
            visible = true,
            processing = true,
            pendingInputPath = "/private/staging/input",
        )

        val actual = DetailFaviconEditorReducer.reduce(
            state,
            DetailMutation.FaviconProcessingFailed(DetailFaviconProcessingError.INVALID_IMAGE),
        )

        assertEquals("/private/staging/input", actual.pendingInputPath)
        assertFalse(actual.processing)
    }

    @Test
    fun iconSaveStartClearsPreviousEditorError() {
        val state = DetailUiState(
            faviconEditor = DetailFaviconEditorState(
                visible = true,
                processingError = DetailFaviconProcessingError.SAVE_FAILED,
            ),
        )

        val actual = DetailReducer.reduce(
            state,
            DetailMutation.SaveStarted(DetailEditCompletion.Icon),
        )

        assertEquals(DetailEditCompletion.Icon, actual.savingEdit)
        assertEquals(null, actual.faviconEditor.processingError)
    }

    @Test
    fun processedPrivateImageEndsProcessingSoSaveCanRun() {
        val state = DetailFaviconEditorState(
            visible = true,
            processing = true,
            initialSource = DetailFaviconSource.InferredDefault,
            source = DetailFaviconSource.InferredDefault,
        )

        val actual = DetailFaviconEditorReducer.reduce(
            state,
            DetailMutation.FaviconSourceChanged(
                DetailFaviconSource.PrivateImage("/private/staging/icon.webp"),
            ),
        )

        assertFalse(actual.processing)
        assertTrue(actual.dirty)
    }

    @Test
    fun openingPrivateImageStartsInCustomImageMode() {
        val actual = DetailFaviconEditorReducer.reduce(
            DetailFaviconEditorState(),
            DetailMutation.FaviconEditorOpened(
                DetailFaviconSource.PrivateImage("/private/images/favicon.webp"),
            ),
        )

        assertEquals("CUSTOM_IMAGE", actual.selectedTab.name)
    }

    @Test
    fun promotedPrivateImageKeepsCandidatePathUntilSaveCompletes() {
        val state = DetailFaviconEditorState(
            visible = true,
            source = DetailFaviconSource.PrivateImage("/private/staging/icon.webp"),
        )

        val promoted = DetailFaviconEditorReducer.reduce(
            state,
            DetailMutation.FaviconSourcePromoted("/private/images/favicon.webp"),
        )

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
                savingEdit = DetailEditCompletion.Icon,
                faviconEditor = promoted,
            ),
            DetailMutation.SaveSucceeded(DetailEditCompletion.Icon),
        )

        assertEquals(DetailFaviconEditorState(), saved.faviconEditor)
    }

    @Test
    fun keepEditingOnlyClearsDiscardConfirmation() {
        val state = DetailFaviconEditorState(
            visible = true,
            confirmDiscard = true,
        )

        val actual = DetailFaviconEditorReducer.reduce(
            state,
            DetailMutation.FaviconEditorDiscardCancelled,
        )

        assertEquals(state.copy(confirmDiscard = false), actual)
    }

    @Test
    fun editorOffersLibraryAndSingleCustomImageMode() {
        assertEquals(
            listOf("ICON_LIBRARY", "CUSTOM_IMAGE"),
            DetailFaviconTab.entries.map { it.name },
        )
    }
}
