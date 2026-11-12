package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.R
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
                initialSource =
                    DetailFaviconSource.BuiltIn(
                        "security.key",
                        DetailFaviconSource.ColorSelection.Automatic,
                    ),
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
                cropInput = DetailFaviconCropInput.Ready("/private/staging/input"),
            )

        val actual =
            DetailFaviconEditorReducer.reduce(
                state,
                DetailMutation.FaviconProcessingFailed(R.string.vault_detail_favicon_error_invalid_image),
            ) as DetailFaviconEditorState.Open

        assertEquals(DetailFaviconCropInput.Ready("/private/staging/input"), actual.cropInput)
        assertEquals(
            DetailFaviconWorkState.Failed(R.string.vault_detail_favicon_error_invalid_image),
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
                showDiscardConfirmation = true,
            )

        val actual =
            DetailFaviconEditorReducer.reduce(
                state,
                DetailMutation.FaviconEditorDiscardCancelled,
            )

        assertEquals(state.copy(showDiscardConfirmation = false), actual)
    }

    @Test
    fun editorOffersLibraryAndSingleCustomImageMode() {
        assertEquals(
            listOf("ICON_LIBRARY", "CUSTOM_IMAGE"),
            DetailFaviconTab.entries.map { it.name },
        )
    }
}
