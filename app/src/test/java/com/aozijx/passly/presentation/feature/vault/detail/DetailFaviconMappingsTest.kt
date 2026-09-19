package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.app.entry.favicon.FaviconDownloadException
import com.aozijx.passly.app.entry.favicon.FaviconDownloadFailure
import com.aozijx.passly.app.entry.favicon.FaviconUrlException
import com.aozijx.passly.app.entry.favicon.FaviconUrlFailure
import org.junit.Assert.assertEquals
import org.junit.Test

class DetailFaviconMappingsTest {

    @Test
    fun faviconFailuresMapToStableUiErrors() {
        assertEquals(
            DetailFaviconProcessingError.INVALID_URL,
            FaviconUrlException(FaviconUrlFailure.HTTPS_REQUIRED).toFaviconProcessingError(),
        )
        assertEquals(
            DetailFaviconProcessingError.URL_NOT_ALLOWED,
            FaviconUrlException(FaviconUrlFailure.PRIVATE_ADDRESS).toFaviconProcessingError(),
        )
        assertEquals(
            DetailFaviconProcessingError.IMAGE_TOO_LARGE,
            FaviconDownloadException(FaviconDownloadFailure.TOO_LARGE).toFaviconProcessingError(),
        )
        assertEquals(
            DetailFaviconProcessingError.INVALID_IMAGE,
            IllegalStateException("network").toFaviconProcessingError(),
        )
    }
}
