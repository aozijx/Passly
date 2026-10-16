package com.aozijx.passly.presentation.feature.settings.main.diagnostics

import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogLine
import com.aozijx.passly.feature.settings.diagnostics.ExportAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsReducerTest {
    private val info = DiagnosticsLogLine(LogLevel.INFO, "info-row", "trace_info", "application.started")
    private val warn = DiagnosticsLogLine(LogLevel.WARN, "warn-row", "trace_shared", "security.auth.failed")
    private val error = DiagnosticsLogLine(
        LogLevel.ERROR,
        "error-row",
        "trace_shared",
        "database.open.failed",
        errorCode = "DATABASE_OPEN_FAILED",
        exceptionKind = "DATABASE_IO",
        exceptionType = "SQLiteException",
    )
    private val fatal = DiagnosticsLogLine(LogLevel.FATAL, "fatal-row", "trace_fatal", "application.crash")
    private val debug = DiagnosticsLogLine(LogLevel.DEBUG, "debug-row", "trace_debug", "autofill.skipped")

    @Test
    fun `default view includes info warn error and fatal while debug is hidden`() {
        val state = DiagnosticsReducer.reduce(
            DiagnosticsUiState(),
            DiagnosticsMutation.LinesLoaded(listOf(info, warn, error, fatal, debug)),
        )

        assertEquals(setOf(LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR), state.selectedLevels)
        assertEquals(listOf(info, warn, error, fatal), state.visibleLines)
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
    fun `search matches trace event error code and exception metadata`() {
        val initial = DiagnosticsUiState(retainedLines = listOf(info, warn, error, debug))

        val byTrace = DiagnosticsReducer.reduce(
            initial,
            DiagnosticsMutation.SearchChanged("trace_shared"),
        )
        val byEvent = DiagnosticsReducer.reduce(initial, DiagnosticsMutation.SearchChanged("database.open"))
        val byCode = DiagnosticsReducer.reduce(initial, DiagnosticsMutation.SearchChanged("database_open_failed"))
        val byClass = DiagnosticsReducer.reduce(initial, DiagnosticsMutation.SearchChanged("sqlite"))
        val cleared = DiagnosticsReducer.reduce(byTrace, DiagnosticsMutation.SearchChanged(""))

        assertEquals(listOf(warn, error), byTrace.visibleLines)
        assertEquals(listOf(error), byEvent.visibleLines)
        assertEquals(listOf(error), byCode.visibleLines)
        assertEquals(listOf(error), byClass.visibleLines)
        assertEquals(listOf(info, warn, error), cleared.visibleLines)
    }

    @Test
    fun `clear completion drops retained and visible diagnostics`() {
        val state = DiagnosticsReducer.reduce(
            DiagnosticsUiState(
                retainedLines = listOf(error),
                isClearConfirmationVisible = true,
            ),
            DiagnosticsMutation.LogsCleared,
        )

        assertTrue(state.retainedLines.isEmpty())
        assertTrue(state.visibleLines.isEmpty())
        assertFalse(state.isClearConfirmationVisible)
    }
}
