package com.aozijx.passly.feature.settings.general

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsSettingsReducerTest {

    @Test
    fun `loaded page is accepted only while viewer is open`() {
        val mutation = DiagnosticsSettingsMutation.LogPageLoaded("line", 4)
        val closed = DiagnosticsSettingsReducer.reduce(DiagnosticsSettingsUiState(), mutation)
        val opened = DiagnosticsSettingsReducer.reduce(
            DiagnosticsSettingsUiState(isViewerOpen = true),
            mutation,
        )

        assertNull(closed.logContent)
        assertEquals("line", opened.logContent)
        assertEquals(4, opened.logByteCount)
    }

    @Test
    fun `closing viewer drops content but preserves measured size`() {
        val result = DiagnosticsSettingsReducer.reduce(
            DiagnosticsSettingsUiState(
                isViewerOpen = true,
                logContent = "sensitive diagnostics",
                logByteCount = 21,
            ),
            DiagnosticsSettingsMutation.ViewerClosed,
        )

        assertFalse(result.isViewerOpen)
        assertNull(result.logContent)
        assertEquals(21, result.logByteCount)
    }

    @Test
    fun `clear completion resets content size and confirmation`() {
        val result = DiagnosticsSettingsReducer.reduce(
            DiagnosticsSettingsUiState(
                isViewerOpen = true,
                logContent = "content",
                logByteCount = 7,
                isClearConfirmationOpen = true,
            ),
            DiagnosticsSettingsMutation.LogsCleared,
        )

        assertTrue(result.isViewerOpen)
        assertNull(result.logContent)
        assertEquals(0, result.logByteCount)
        assertFalse(result.isClearConfirmationOpen)
    }
}
