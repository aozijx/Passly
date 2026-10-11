package com.aozijx.passly.presentation.feature.settings.main.general

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class DiagnosticsSettingsReducerTest {
    @Test
    fun `loaded page is accepted only while viewer is open`() {
        val mutation = DiagnosticsSettingsMutation.LogPageLoaded("line", 4)
        val closed = DiagnosticsSettingsReducer.reduce(DiagnosticsSettingsUiState(), mutation)
        val opened = DiagnosticsSettingsReducer.reduce(
            DiagnosticsSettingsUiState(
                overlay = DiagnosticsOverlayUiState.Viewer(DiagnosticsLogPageUiState.Loading),
            ),
            mutation,
        )

        assertSame(DiagnosticsOverlayUiState.Closed, closed.overlay)
        assertEquals(
            DiagnosticsLogPageUiState.Content("line"),
            (opened.overlay as DiagnosticsOverlayUiState.Viewer).page,
        )
        assertEquals(4, opened.logByteCount)
    }

    @Test
    fun `closing viewer drops content but preserves measured size`() {
        val result = DiagnosticsSettingsReducer.reduce(
            DiagnosticsSettingsUiState(
                logByteCount = 21,
                overlay = DiagnosticsOverlayUiState.Viewer(
                    DiagnosticsLogPageUiState.Content("sensitive diagnostics"),
                ),
            ),
            DiagnosticsSettingsMutation.ViewerClosed,
        )

        assertSame(DiagnosticsOverlayUiState.Closed, result.overlay)
        assertEquals(21, result.logByteCount)
    }

    @Test
    fun `clear completion resets size and closes the active overlay`() {
        val result = DiagnosticsSettingsReducer.reduce(
            DiagnosticsSettingsUiState(
                logByteCount = 7,
                overlay = DiagnosticsOverlayUiState.ClearConfirmation,
            ),
            DiagnosticsSettingsMutation.LogsCleared,
        )

        assertEquals(0, result.logByteCount)
        assertSame(DiagnosticsOverlayUiState.Closed, result.overlay)
    }
}
