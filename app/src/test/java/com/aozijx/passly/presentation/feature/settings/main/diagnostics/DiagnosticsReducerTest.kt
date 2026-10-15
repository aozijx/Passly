package com.aozijx.passly.presentation.feature.settings.main.diagnostics

import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogLine
import com.aozijx.passly.feature.settings.diagnostics.ExportAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsReducerTest {
    private val info = DiagnosticsLogLine(LogLevel.INFO, "info-row", "trace_info")
    private val warn = DiagnosticsLogLine(LogLevel.WARN, "warn-row", "trace_shared")
    private val error = DiagnosticsLogLine(LogLevel.ERROR, "error-row", "trace_shared")
    private val fatal = DiagnosticsLogLine(LogLevel.FATAL, "fatal-row", "trace_fatal")
    private val debug = DiagnosticsLogLine(LogLevel.DEBUG, "debug-row", "trace_debug")

    @Test
    fun `default view includes info warn error and fatal while debug is hidden`() {
        val state = DiagnosticsReducer.reduce(
            DiagnosticsUiState(),
            DiagnosticsMutation.LinesLoaded(listOf(info, warn, error, fatal, debug), 42),
        )

        assertEquals(setOf(LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR), state.selectedLevels)
        assertEquals(listOf(info, warn, error, fatal), state.visibleLines)
        assertEquals(42, state.logByteCount)
        assertFalse(state.isLoading)
    }

    @Test
    fun `level toggle changes only visible lines and preserves export selection`() {
        val initial = DiagnosticsUiState(
            retainedLines = listOf(info, warn, error, debug),
            pendingExportAction = ExportAction.SHARE,
        )

        val state = DiagnosticsReducer.reduce(
            initial,
            DiagnosticsMutation.LevelToggled(LogLevel.INFO),
        )

        assertEquals(listOf(warn, error), state.visibleLines)
        assertEquals(ExportAction.SHARE, state.pendingExportAction)
    }

    @Test
    fun `exact trace search selects the whole retained trace and clear restores level filtering`() {
        val initial = DiagnosticsUiState(retainedLines = listOf(info, warn, error, debug))

        val searched = DiagnosticsReducer.reduce(
            initial,
            DiagnosticsMutation.SearchChanged("trace_shared"),
        )
        val cleared = DiagnosticsReducer.reduce(searched, DiagnosticsMutation.SearchChanged(""))

        assertEquals(listOf(warn, error), searched.visibleLines)
        assertEquals(listOf(info, warn, error), cleared.visibleLines)
    }

    @Test
    fun `clear completion drops retained and visible diagnostics`() {
        val state = DiagnosticsReducer.reduce(
            DiagnosticsUiState(
                retainedLines = listOf(error),
                logByteCount = 10,
                isClearConfirmationVisible = true,
            ),
            DiagnosticsMutation.LogsCleared,
        )

        assertTrue(state.retainedLines.isEmpty())
        assertTrue(state.visibleLines.isEmpty())
        assertEquals(0, state.logByteCount)
        assertFalse(state.isClearConfirmationVisible)
    }
}
