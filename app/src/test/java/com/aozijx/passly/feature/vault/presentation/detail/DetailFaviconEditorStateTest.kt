package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailFaviconEditorStateTest {
    @Test
    fun `favicon editor state exposes its display projection directly`() {
        val actual =
            DetailFaviconEditorState.Open(
                initialSource = DetailFaviconSource.InferredDefault,
                source = DetailFaviconSource.PrivateImage("/private/icon.webp"),
                selectedTab = DetailFaviconTab.CUSTOM_IMAGE,
                work =
                    DetailFaviconWorkState.Failed(
                        R.string.vault_detail_favicon_error_invalid_image,
                    ),
            )

        assertTrue(actual.dirty)
        assertEquals(
            DetailFaviconSource.PrivateImage("/private/icon.webp"),
            actual.source,
        )
        assertEquals(DetailFaviconTab.CUSTOM_IMAGE, actual.selectedTab)
        assertFalse(actual.canSubmitImageUrl)
        assertEquals(
            DetailFaviconWorkState.Failed(R.string.vault_detail_favicon_error_invalid_image),
            actual.work,
        )
    }

    @Test
    fun `nonempty image URL is submitted for validation by the processor`() {
        val actual =
            DetailFaviconEditorState.Open(
                initialSource = DetailFaviconSource.InferredDefault,
                imageUrl = "not a URL",
            )

        assertTrue(actual.canSubmitImageUrl)
    }
}
