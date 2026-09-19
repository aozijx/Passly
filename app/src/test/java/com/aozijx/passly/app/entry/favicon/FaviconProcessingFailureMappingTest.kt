package com.aozijx.passly.app.entry.favicon

import com.aozijx.passly.core.platform.media.FaviconProcessingFailure
import org.junit.Assert.assertEquals
import org.junit.Test

class FaviconProcessingFailureMappingTest {
    @Test
    fun `implementation failures map to stable media contract`() {
        assertEquals(
            FaviconProcessingFailure.INVALID_URL,
            FaviconUrlException(FaviconUrlFailure.HTTPS_REQUIRED)
                .toFaviconProcessingException().failure,
        )
        assertEquals(
            FaviconProcessingFailure.URL_NOT_ALLOWED,
            FaviconUrlException(FaviconUrlFailure.PRIVATE_ADDRESS)
                .toFaviconProcessingException().failure,
        )
        assertEquals(
            FaviconProcessingFailure.IMAGE_TOO_LARGE,
            FaviconDownloadException(FaviconDownloadFailure.TOO_LARGE)
                .toFaviconProcessingException().failure,
        )
        assertEquals(
            FaviconProcessingFailure.INVALID_IMAGE,
            IllegalStateException("decode").toFaviconProcessingException().failure,
        )
    }
}
