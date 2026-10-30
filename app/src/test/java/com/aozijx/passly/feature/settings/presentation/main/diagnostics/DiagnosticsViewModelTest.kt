package com.aozijx.passly.feature.settings.presentation.main.diagnostics

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
import com.aozijx.passly.core.observability.sink.LogClearResult
import com.aozijx.passly.core.observability.trace.TraceToken
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.clipboard.port.SensitiveClipboardWriter
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsExportGateway
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsExportRequest
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogLine
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogStore
import com.aozijx.passly.feature.settings.diagnostics.ExportAction
import com.aozijx.passly.feature.settings.diagnostics.ExportDiagnosticsUseCase
import com.aozijx.passly.feature.settings.diagnostics.LogFormat
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
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
    fun `page entry refreshes typed lines`() = runTest {
        val lines = listOf(
            DiagnosticsLogLine(LogLevel.INFO, "row-one", "trace_one", "application.one"),
            DiagnosticsLogLine(LogLevel.WARN, "row-two", "trace_two", "application.two"),
        )
        val store = RecordingLogStore(lines)
        val viewModel = DiagnosticsViewModel(
            secureSessionAccessState = FullAccess,
            logStore = store,
            exportDiagnostics = ExportDiagnosticsUseCase(FullAccess, UnusedAuthorizationGate, UnusedExportGateway),
            logger = UnusedLogger,
            clipboardWriter = RecordingClipboardWriter(),
        )

        viewModel.onAction(DiagnosticsAction.PageEntered)
        runCurrent()

        assertEquals(1, store.readCount)
        assertEquals(lines, viewModel.uiState.value.retainedLines)
    }

    @Test
    fun `failed clear reports failure and keeps retained lines`() = runTest {
        val lines = listOf(
            DiagnosticsLogLine(LogLevel.INFO, "row-one", "trace_one", "application.one"),
        )
        val store = RecordingLogStore(lines, clearResult = LogClearResult.Failed)
        val viewModel = viewModel(store)
        viewModel.onAction(DiagnosticsAction.PageEntered)
        runCurrent()
        val effect = async { viewModel.effects.first() }

        viewModel.onAction(DiagnosticsAction.RequestClear)
        viewModel.onAction(DiagnosticsAction.ConfirmClear)
        runCurrent()

        assertEquals(DiagnosticsEffect.ClearFailed, effect.await())
        assertEquals(lines, viewModel.uiState.value.retainedLines)
        assertEquals(false, viewModel.uiState.value.isClearConfirmationVisible)
    }

    @Test
    fun `copy row and trace use the sensitive clipboard writer with bounded rows`() = runTest {
        val traceLines = (0..100).map { index ->
            DiagnosticsLogLine(LogLevel.INFO, "row-$index", "trace_shared", "application.row")
        }
        val clipboard = RecordingClipboardWriter()
        val viewModel = viewModel(
            store = RecordingLogStore(traceLines),
            clipboardWriter = clipboard,
        )
        viewModel.onAction(DiagnosticsAction.PageEntered)
        runCurrent()

        viewModel.onAction(DiagnosticsAction.CopyRow(traceLines.first()))
        runCurrent()
        assertEquals(listOf("row-0"), clipboard.values)

        viewModel.onAction(DiagnosticsAction.CopyTrace("trace_shared"))
        runCurrent()
        val copied = clipboard.values.last()
        assertEquals(100, copied.lineSequence().count())
        assertEquals("row-0", copied.lineSequence().first())
        assertEquals("row-99", copied.lineSequence().last())
    }

    @Test
    fun `page exit prevents a suspended diagnostics copy from publishing`() = runTest {
        val clipboard = DelayedClipboardWriter()
        val lines = listOf(DiagnosticsLogLine(LogLevel.INFO, "row-one", "trace_one", "application.one"))
        val viewModel = viewModel(RecordingLogStore(lines), clipboardWriter = clipboard)
        viewModel.onAction(DiagnosticsAction.PageEntered)
        runCurrent()

        viewModel.onAction(DiagnosticsAction.CopyRow(lines.single()))
        runCurrent()
        clipboard.writeStarted.await()
        viewModel.onAction(DiagnosticsAction.PageExited)
        clipboard.finishWrite.complete(Unit)
        runCurrent()

        assertEquals(emptyList<String>(), clipboard.values)
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
                fileName = "passly_logs.tsv",
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
        clipboardWriter: SensitiveClipboardWriter = SensitiveClipboardWriter {},
    ) = DiagnosticsViewModel(
        secureSessionAccessState = FullAccess,
        logStore = store,
        exportDiagnostics = ExportDiagnosticsUseCase(FullAccess, gate, gateway),
        logger = logger,
        clipboardWriter = clipboardWriter,
    )

    private class RecordingLogStore(
        private val lines: List<DiagnosticsLogLine>,
        private val clearResult: LogClearResult = LogClearResult.Cleared,
    ) : DiagnosticsLogStore {
        var readCount = 0

        override suspend fun readLines(limit: Int): List<DiagnosticsLogLine> {
            readCount++
            return lines.take(limit)
        }

        override suspend fun storageBytes(): Long = 1_024L

        override suspend fun clear() = clearResult
    }

    private class RecordingClipboardWriter : SensitiveClipboardWriter {
        val values = mutableListOf<String>()

        override suspend fun writeSensitive(text: String) {
            values += text
        }
    }

    private class DelayedClipboardWriter : SensitiveClipboardWriter {
        val writeStarted = CompletableDeferred<Unit>()
        val finishWrite = CompletableDeferred<Unit>()
        val values = mutableListOf<String>()

        override suspend fun writeSensitive(text: String) {
            values += text
        }

        override suspend fun writeSensitiveIf(text: String, canWrite: () -> Boolean): Boolean =
            withContext(NonCancellable) {
                writeStarted.complete(Unit)
                finishWrite.await()
                if (!canWrite()) return@withContext false
                values += text
                true
            }
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
