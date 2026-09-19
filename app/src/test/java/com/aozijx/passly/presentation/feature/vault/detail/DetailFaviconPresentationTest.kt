package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconDraftSourceUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconEditorTabUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconProcessingErrorUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailFaviconPresentationTest {
    @Test
    fun `favicon editor state maps to ui only at the presentation boundary`() {
        val state = DetailFaviconEditorState(
            visible = true,
            initialSource = DetailFaviconSource.InferredDefault,
            source = DetailFaviconSource.PrivateImage("/private/icon.webp"),
            selectedTab = DetailFaviconTab.CUSTOM_IMAGE,
            processingError = DetailFaviconProcessingError.INVALID_IMAGE,
        )

        val actual = state.toFaviconEditorUiModel()

        assertTrue(actual.visible)
        assertTrue(actual.dirty)
        assertEquals(
            FaviconDraftSourceUiModel.PrivateImage("/private/icon.webp"),
            actual.source,
        )
        assertEquals(FaviconEditorTabUiModel.CUSTOM_IMAGE, actual.selectedTab)
        assertEquals(FaviconProcessingErrorUiModel.INVALID_IMAGE, actual.processingError)
    }
}
