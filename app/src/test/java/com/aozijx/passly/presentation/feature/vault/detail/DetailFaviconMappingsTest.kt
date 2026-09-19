package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.core.platform.media.FaviconProcessingException
import com.aozijx.passly.core.platform.media.FaviconProcessingFailure
import org.junit.Assert.assertEquals
import org.junit.Test

class DetailFaviconMappingsTest {

    @Test
    fun faviconFailuresMapToStableUiErrors() {
        assertEquals(
            DetailFaviconProcessingError.INVALID_URL,
            FaviconProcessingException(FaviconProcessingFailure.INVALID_URL).toFaviconProcessingError(),
        )
        assertEquals(
            DetailFaviconProcessingError.URL_NOT_ALLOWED,
            FaviconProcessingException(FaviconProcessingFailure.URL_NOT_ALLOWED).toFaviconProcessingError(),
        )
        assertEquals(
            DetailFaviconProcessingError.IMAGE_TOO_LARGE,
            FaviconProcessingException(FaviconProcessingFailure.IMAGE_TOO_LARGE).toFaviconProcessingError(),
        )
        assertEquals(
            DetailFaviconProcessingError.INVALID_IMAGE,
            IllegalStateException("network").toFaviconProcessingError(),
        )
    }
}
