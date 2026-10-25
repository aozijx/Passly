package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailFaviconEditorUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.FaviconDraftSourceUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.FaviconEditorTabUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.FaviconWorkUiState
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.FaviconProcessingErrorUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailFaviconPresentationTest {
    @Test
    fun `favicon editor state maps to ui only at the presentation boundary`() {
        val state = DetailFaviconEditorState.Open(
            initialSource = DetailFaviconSource.InferredDefault,
            source = DetailFaviconSource.PrivateImage("/private/icon.webp"),
            selectedTab = DetailFaviconTab.CUSTOM_IMAGE,
            work = DetailFaviconWorkState.Failed(
                DetailFaviconProcessingError.INVALID_IMAGE,
            ),
        )

        val actual = state.toFaviconEditorUiModel() as DetailFaviconEditorUiModel.Visible

        assertTrue(actual.dirty)
        assertEquals(
            FaviconDraftSourceUiModel.PrivateImage("/private/icon.webp"),
            actual.source,
        )
        assertEquals(FaviconEditorTabUiModel.CUSTOM_IMAGE, actual.selectedTab)
        assertEquals(
            FaviconWorkUiState.Failed(FaviconProcessingErrorUiModel.INVALID_IMAGE),
            actual.work,
        )
    }
}
