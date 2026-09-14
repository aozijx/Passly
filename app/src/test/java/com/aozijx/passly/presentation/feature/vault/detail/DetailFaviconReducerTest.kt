package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.presentation.ui.vault.detail.model.DetailFaviconEditorUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconDraftSourceUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconEditorTabUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconProcessingErrorUiModel
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
                FaviconDraftSourceUiModel.BuiltIn("security.key", "primary"),
            ),
        )

        assertTrue(actual.faviconEditor.visible)
        assertEquals(
            FaviconDraftSourceUiModel.BuiltIn("security.key", "primary"),
            actual.faviconEditor.source,
        )
        assertFalse(actual.faviconEditor.dirty)
    }

    @Test
    fun selectingDefaultMakesBuiltInDraftDirty() {
        val opened = DetailFaviconEditorUiModel(
            visible = true,
            initialSource = FaviconDraftSourceUiModel.BuiltIn("security.key", null),
            source = FaviconDraftSourceUiModel.BuiltIn("security.key", null),
        )

        val actual = DetailFaviconEditorReducer.reduce(
            opened,
            DetailMutation.FaviconSourceChanged(FaviconDraftSourceUiModel.InferredDefault),
        )

        assertEquals(FaviconDraftSourceUiModel.InferredDefault, actual.source)
        assertTrue(actual.dirty)
    }

    @Test
    fun dirtyDismissConfirmsAndFailurePreservesDraftWhileSuccessCloses() {
        val editor = DetailFaviconEditorUiModel(
            visible = true,
            initialSource = FaviconDraftSourceUiModel.InferredDefault,
            source = FaviconDraftSourceUiModel.BuiltIn("finance.bank", "secondary"),
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
        assertEquals(FaviconProcessingErrorUiModel.SAVE_FAILED, failed.faviconEditor.processingError)
        assertEquals(DetailFaviconEditorUiModel(), succeeded.faviconEditor)
    }

    @Test
    fun processingFailurePreservesPendingCropInput() {
        val state = DetailFaviconEditorUiModel(
            visible = true,
            processing = true,
            pendingInputPath = "/private/staging/input",
        )

        val actual = DetailFaviconEditorReducer.reduce(
            state,
            DetailMutation.FaviconProcessingFailed(FaviconProcessingErrorUiModel.INVALID_IMAGE),
        )

        assertEquals("/private/staging/input", actual.pendingInputPath)
        assertFalse(actual.processing)
    }

    @Test
    fun iconSaveStartClearsPreviousEditorError() {
        val state = DetailUiState(
            faviconEditor = DetailFaviconEditorUiModel(
                visible = true,
                processingError = FaviconProcessingErrorUiModel.SAVE_FAILED,
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
        val state = DetailFaviconEditorUiModel(
            visible = true,
            processing = true,
            initialSource = FaviconDraftSourceUiModel.InferredDefault,
            source = FaviconDraftSourceUiModel.InferredDefault,
        )

        val actual = DetailFaviconEditorReducer.reduce(
            state,
            DetailMutation.FaviconSourceChanged(
                FaviconDraftSourceUiModel.PrivateImage("/private/staging/icon.webp"),
            ),
        )

        assertFalse(actual.processing)
        assertTrue(actual.dirty)
    }

    @Test
    fun openingPrivateImageStartsInCustomImageMode() {
        val actual = DetailFaviconEditorReducer.reduce(
            DetailFaviconEditorUiModel(),
            DetailMutation.FaviconEditorOpened(
                FaviconDraftSourceUiModel.PrivateImage("/private/images/favicon.webp"),
            ),
        )

        assertEquals("CUSTOM_IMAGE", actual.selectedTab.name)
    }

    @Test
    fun promotedPrivateImageKeepsCandidatePathUntilSaveCompletes() {
        val state = DetailFaviconEditorUiModel(
            visible = true,
            source = FaviconDraftSourceUiModel.PrivateImage("/private/staging/icon.webp"),
        )

        val promoted = DetailFaviconEditorReducer.reduce(
            state,
            DetailMutation.FaviconSourcePromoted("/private/images/favicon.webp"),
        )

        assertEquals(
            FaviconDraftSourceUiModel.PrivateImage("/private/images/favicon.webp"),
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

        assertEquals(DetailFaviconEditorUiModel(), saved.faviconEditor)
    }

    @Test
    fun keepEditingOnlyClearsDiscardConfirmation() {
        val state = DetailFaviconEditorUiModel(
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
            FaviconEditorTabUiModel.entries.map { it.name },
        )
    }
}
