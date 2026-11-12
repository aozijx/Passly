package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.R
import com.aozijx.passly.core.platform.media.FaviconProcessingException
import com.aozijx.passly.core.platform.media.FaviconProcessingFailure
import com.aozijx.passly.domain.entry.model.EntryIcon
import org.junit.Assert.assertEquals
import org.junit.Test

class DetailFaviconMappingsTest {
    @Test
    fun faviconFailuresMapToLocalizedMessages() {
        assertEquals(
            R.string.vault_detail_favicon_error_invalid_url,
            FaviconProcessingException(FaviconProcessingFailure.INVALID_URL).toFaviconProcessingMessageRes(),
        )
        assertEquals(
            R.string.vault_detail_favicon_error_url_not_allowed,
            FaviconProcessingException(FaviconProcessingFailure.URL_NOT_ALLOWED).toFaviconProcessingMessageRes(),
        )
        assertEquals(
            R.string.vault_detail_favicon_error_too_large,
            FaviconProcessingException(FaviconProcessingFailure.IMAGE_TOO_LARGE).toFaviconProcessingMessageRes(),
        )
        assertEquals(
            R.string.vault_detail_favicon_error_invalid_image,
            IllegalStateException("network").toFaviconProcessingMessageRes(),
        )
    }

    @Test
    fun automaticColorMapsToAbsentDomainOverride() {
        val source =
            DetailFaviconSource.BuiltIn(
                key = "security.key",
                color = DetailFaviconSource.ColorSelection.Automatic,
            )

        assertEquals(EntryIcon(name = "security.key"), source.toEntryIcon())
        assertEquals(source, EntryIcon(name = "security.key").toDetailFaviconSource())
    }

    @Test
    fun customColorTokenRoundTripsWithoutNormalization() {
        val entryIcon = EntryIcon(name = "security.key", color = "legacy-custom-token")
        val source = entryIcon.toDetailFaviconSource()

        assertEquals(
            DetailFaviconSource.BuiltIn(
                "security.key",
                DetailFaviconSource.ColorSelection.Token("legacy-custom-token"),
            ),
            source,
        )
        assertEquals(entryIcon, source.toEntryIcon())
    }
}
