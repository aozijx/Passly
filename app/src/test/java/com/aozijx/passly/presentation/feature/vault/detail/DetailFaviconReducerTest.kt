package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.presentation.ui.vault.detail.model.DetailFaviconEditorUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconDraftSourceUiModel
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
        val opened = DetailUiState(
            faviconEditor = DetailFaviconEditorUiModel(
                visible = true,
                initialSource = FaviconDraftSourceUiModel.BuiltIn("security.key", null),
                source = FaviconDraftSourceUiModel.BuiltIn("security.key", null),
            ),
        )

        val actual = DetailReducer.reduce(
            opened,
            DetailMutation.FaviconSourceChanged(FaviconDraftSourceUiModel.InferredDefault),
        )

        assertEquals(FaviconDraftSourceUiModel.InferredDefault, actual.faviconEditor.source)
        assertTrue(actual.faviconEditor.dirty)
    }

    @Test
    fun dirtyDismissConfirmsAndFailurePreservesDraftWhileSuccessCloses() {
        val editor = DetailFaviconEditorUiModel(
            visible = true,
            initialSource = FaviconDraftSourceUiModel.InferredDefault,
            source = FaviconDraftSourceUiModel.BuiltIn("finance.bank", "secondary"),
        )
        val dirty = DetailUiState(faviconEditor = editor)
        val requested = DetailReducer.reduce(
            dirty,
            DetailMutation.FaviconEditorDismissRequested,
        )
        val saving = requested.copy(savingEdit = DetailEditCompletion.Icon)

        val failed = DetailReducer.reduce(
            saving,
            DetailMutation.SaveFailed(DetailEditCompletion.Icon, "CONFLICT"),
        )
        val succeeded = DetailReducer.reduce(
            saving,
            DetailMutation.SaveSucceeded(DetailEditCompletion.Icon),
        )

        assertTrue(requested.faviconEditor.confirmDiscard)
        assertEquals(editor.source, failed.faviconEditor.source)
        assertEquals(DetailFaviconEditorUiModel(), succeeded.faviconEditor)
    }
}
