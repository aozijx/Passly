package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.app.entry.favicon.FaviconDownloadException
import com.aozijx.passly.app.entry.favicon.FaviconDownloadFailure
import com.aozijx.passly.app.entry.favicon.FaviconUrlException
import com.aozijx.passly.app.entry.favicon.FaviconUrlFailure
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconProcessingErrorUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class DetailFaviconMappingsTest {

    @Test
    fun faviconFailuresMapToStableUiErrors() {
        assertEquals(
            FaviconProcessingErrorUiModel.INVALID_URL,
            FaviconUrlException(FaviconUrlFailure.HTTPS_REQUIRED).toFaviconUiError(),
        )
        assertEquals(
            FaviconProcessingErrorUiModel.URL_NOT_ALLOWED,
            FaviconUrlException(FaviconUrlFailure.PRIVATE_ADDRESS).toFaviconUiError(),
        )
        assertEquals(
            FaviconProcessingErrorUiModel.IMAGE_TOO_LARGE,
            FaviconDownloadException(FaviconDownloadFailure.TOO_LARGE).toFaviconUiError(),
        )
        assertEquals(
            FaviconProcessingErrorUiModel.INVALID_IMAGE,
            IllegalStateException("network").toFaviconUiError(),
        )
    }
}
