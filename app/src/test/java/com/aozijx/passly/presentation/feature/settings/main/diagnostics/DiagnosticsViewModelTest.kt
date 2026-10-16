package com.aozijx.passly.presentation.feature.settings.main.diagnostics

import com.aozijx.passly.core.observability.ErrorCode
import com.aozijx.passly.core.observability.EventKey
import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.TraceResumeResult
import com.aozijx.passly.core.observability.TraceScope
import com.aozijx.passly.core.observability.trace.TraceToken
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsExportGateway
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsExportRequest
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogLine
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogStore
import com.aozijx.passly.feature.settings.diagnostics.ExportAction
import com.aozijx.passly.feature.settings.diagnostics.ExportDiagnosticsUseCase
import com.aozijx.passly.feature.settings.diagnostics.LogFormat
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiagnosticsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `page entry refreshes typed lines and cached byte count`() = runTest {
        val lines = listOf(
            DiagnosticsLogLine(LogLevel.INFO, "row-one", "trace_one"),
            DiagnosticsLogLine(LogLevel.WARN, "row-two", "trace_two"),
        )
        val store = RecordingLogStore(lines)
        val viewModel = DiagnosticsViewModel(
            secureSessionAccessState = FullAccess,
            logStore = store,
            exportDiagnostics = ExportDiagnosticsUseCase(FullAccess, UnusedAuthorizationGate, UnusedExportGateway),
            logger = UnusedLogger,
        )

        viewModel.onAction(DiagnosticsAction.PageEntered)
        runCurrent()

        assertEquals(1, store.readCount)
        assertEquals(lines, viewModel.uiState.value.retainedLines)
        assertEquals(1_024L, viewModel.uiState.value.logByteCount)
    }

    @Test
    fun `copy row and trace emit only sanitized bounded tsv rows`() = runTest {
        val traceLines = (0..100).map { index ->
            DiagnosticsLogLine(LogLevel.INFO, "row-$index", "trace_shared")
        }
        val viewModel = viewModel(RecordingLogStore(traceLines))
        viewModel.onAction(DiagnosticsAction.PageEntered)
        runCurrent()

        val rowEffect = async { viewModel.effects.first() }
        viewModel.onAction(DiagnosticsAction.CopyRow(traceLines.first()))
        assertEquals(DiagnosticsEffect.CopyText("row-0"), rowEffect.await())

        val traceEffect = async { viewModel.effects.first() }
        viewModel.onAction(DiagnosticsAction.CopyTrace("trace_shared"))
        val copied = (traceEffect.await() as DiagnosticsEffect.CopyText).text
        assertEquals(100, copied.lineSequence().count())
        assertEquals("row-0", copied.lineSequence().first())
        assertEquals("row-99", copied.lineSequence().last())
    }

    @Test
    fun `local format selection emits matching saf contract independent of level filters`() = runTest {
        val viewModel = viewModel(RecordingLogStore(emptyList()))
        viewModel.onAction(DiagnosticsAction.ToggleLevel(LogLevel.INFO))
        viewModel.onAction(DiagnosticsAction.RequestExport(ExportAction.EXPORT_LOCAL))

        val effect = async { viewModel.effects.first() }
        viewModel.onAction(DiagnosticsAction.SelectFormat(LogFormat.TSV))

        assertEquals(
            DiagnosticsEffect.SelectSaveDocument(
                format = LogFormat.TSV,
                mimeType = "text/tab-separated-values",
                fileName = "passly_diagnostics.tsv",
            ),
            effect.await(),
        )
        assertEquals(LogFormat.TSV, viewModel.uiState.value.pendingLocalFormat)

        viewModel.onAction(DiagnosticsAction.SaveDocumentCancelled)

        assertEquals(null, viewModel.uiState.value.pendingLocalFormat)
    }

    @Test
    fun `share format reaches authenticated gateway without viewer filters`() = runTest {
        val gateway = RecordingExportGateway()
        val viewModel = viewModel(
            store = RecordingLogStore(emptyList()),
            gate = AllowingAuthorizationGate,
            gateway = gateway,
            logger = ExecutingLogger,
        )
        viewModel.onAction(DiagnosticsAction.SearchChanged("trace_missing"))
        viewModel.onAction(DiagnosticsAction.RequestExport(ExportAction.SHARE))
        viewModel.onAction(DiagnosticsAction.SelectFormat(LogFormat.ZIP))
        runCurrent()

        assertEquals(DiagnosticsExportRequest(ExportAction.SHARE, LogFormat.ZIP), gateway.request)
    }

    private fun viewModel(
        store: DiagnosticsLogStore,
        gate: AuthorizationGate = UnusedAuthorizationGate,
        gateway: DiagnosticsExportGateway = UnusedExportGateway,
        logger: Logger = UnusedLogger,
    ) = DiagnosticsViewModel(
        secureSessionAccessState = FullAccess,
        logStore = store,
        exportDiagnostics = ExportDiagnosticsUseCase(FullAccess, gate, gateway),
        logger = logger,
    )

    private class RecordingLogStore(private val lines: List<DiagnosticsLogLine>) : DiagnosticsLogStore {
        var readCount = 0

        override suspend fun readLines(limit: Int): List<DiagnosticsLogLine> {
            readCount++
            return lines.take(limit)
        }

        override suspend fun storageBytes(): Long = 1_024L

        override suspend fun clear() = Unit
    }

    private object FullAccess : SecureSessionAccessState {
        override val authenticationState: StateFlow<AuthenticationState> =
            MutableStateFlow(AuthenticationState.Authenticated(1L))

        override fun isUnlocked() = true
    }

    private object UnusedAuthorizationGate : AuthorizationGate {
        override suspend fun <T> authorize(
            scope: AuthorizationScope,
            input: AuthInput,
            block: suspend (AuthorizationPermit) -> T,
        ): AuthorizationResult<T> = error("Authorization is not used")
    }

    private object AllowingAuthorizationGate : AuthorizationGate {
        override suspend fun <T> authorize(
            scope: AuthorizationScope,
            input: AuthInput,
            block: suspend (AuthorizationPermit) -> T,
        ): AuthorizationResult<T> = AuthorizationResult.Allowed(block(object : AuthorizationPermit {}))
    }

    private object UnusedExportGateway : DiagnosticsExportGateway {
        override suspend fun export(
            permit: AuthorizationPermit,
            request: DiagnosticsExportRequest,
        ): Result<Unit> = error("Export is not used")

        override suspend fun discard(request: DiagnosticsExportRequest) = Unit
    }

    private class RecordingExportGateway : DiagnosticsExportGateway {
        var request: DiagnosticsExportRequest? = null
        override suspend fun export(
            permit: AuthorizationPermit,
            request: DiagnosticsExportRequest,
        ): Result<Unit> {
            this.request = request
            return Result.success(Unit)
        }

        override suspend fun discard(request: DiagnosticsExportRequest) = Unit
    }

    private object UnusedLogger : Logger {
        override fun debug(event: EventKey, fields: LogFields) = error("Logger is not used")
        override fun info(event: EventKey, fields: LogFields) = error("Logger is not used")
        override fun degraded(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, fields: LogFields) =
            error("Logger is not used")

        override fun warn(
            event: EventKey,
            errorCode: ErrorCode,
            reason: ReasonCode,
            throwable: Throwable,
            fields: LogFields,
        ) = error("Logger is not used")

        override fun error(
            event: EventKey,
            errorCode: ErrorCode,
            reason: ReasonCode,
            throwable: Throwable,
            fields: LogFields,
        ) = error("Logger is not used")

        override fun fatal(
            event: EventKey,
            errorCode: ErrorCode,
            reason: ReasonCode,
            throwable: Throwable,
            fields: LogFields,
        ) = error("Logger is not used")

        override suspend fun <T> withRootTrace(
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ): T = error("Logger is not used")

        override suspend fun <T> inSpan(
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ): T = error("Logger is not used")

        override fun captureTraceToken(singleUse: Boolean): TraceToken = error("Logger is not used")
        override suspend fun <T> resumeTrace(
            token: TraceToken,
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ): TraceResumeResult<T> = error("Logger is not used")
    }

    private object ExecutingLogger : Logger by UnusedLogger {
        override suspend fun <T> withRootTrace(
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ): T = block(TestTraceScope)
    }

    private object TestTraceScope : TraceScope {
        override val traceId = TraceId("trace_12345678")
        override val spanId = SpanId("span_12345678")
        override val parentSpanId: SpanId? = null
    }
}
