package com.aozijx.passly.feature.vault.presentation.detail

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
                        DetailFaviconProcessingError.INVALID_IMAGE,
                    ),
            )

        assertTrue(actual.dirty)
        assertEquals(
            DetailFaviconSource.PrivateImage("/private/icon.webp"),
            actual.source,
        )
        assertEquals(DetailFaviconTab.CUSTOM_IMAGE, actual.selectedTab)
        assertFalse(actual.canDownloadImageUrl)
        assertEquals(
            DetailFaviconWorkState.Failed(DetailFaviconProcessingError.INVALID_IMAGE),
            actual.work,
        )
    }
}
