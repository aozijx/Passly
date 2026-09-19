package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailContentEvent
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailEditorOverlayEvent
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailFieldUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetailPresentationEventsTest {
    @Test
    fun `content field events map once at feature boundary`() {
        assertEquals(
            DetailUiAction.SaveField(RevealedFieldKey.CVV, "123"),
            DetailContentEvent.SaveField(DetailFieldUiModel.CARD_CVV, "123").toDetailUiAction(),
        )
        assertEquals(
            DetailUiAction.CopyField(FieldKey.SSH_KEY),
            DetailContentEvent.CopyField(DetailFieldUiModel.SSH_PRIVATE_KEY).toDetailUiAction(),
        )
        assertEquals(
            DetailUiAction.RevealFields(setOf(RevealedFieldKey.CARD_NUMBER, RevealedFieldKey.CVV)),
            DetailContentEvent.RevealFields(
                setOf(DetailFieldUiModel.CARD_NUMBER, DetailFieldUiModel.CARD_CVV),
            ).toDetailUiAction(),
        )
    }

    @Test
    fun `route-only events are not converted to feature actions`() {
        assertNull(DetailContentEvent.OpenRelatedEntry("related").toDetailUiAction())
        assertNull(DetailEditorOverlayEvent.UploadFavicon.toDetailUiAction())
    }

    @Test
    fun `overlay save events map to view model actions`() {
        assertEquals(
            DetailUiAction.SaveTags,
            DetailEditorOverlayEvent.SaveTags.toDetailUiAction(),
        )
        assertEquals(
            DetailUiAction.CropFaviconImage(2f, 1f, -1f),
            DetailEditorOverlayEvent.CropFavicon(2f, 1f, -1f).toDetailUiAction(),
        )
    }
}
