package com.aozijx.passly.feature.settings.presentation.main.diagnostics

import com.aozijx.passly.core.observability.ErrorCode
import com.aozijx.passly.core.observability.EventKey
import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.sink.LogClearResult
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.trace.TraceToken
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.TraceResumeResult
import com.aozijx.passly.core.observability.TraceScope
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.AuthInput
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
import java.io.IOException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiagnosticsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `page entry refreshes typed lines`() =
        runTest {
            val lines =
                listOf(
                    DiagnosticsLogLine(LogLevel.INFO, "row-one", "trace_one", "application.one"),
                    DiagnosticsLogLine(LogLevel.WARN, "row-two", "trace_two", "application.two"),
                )
            val store = RecordingLogStore(lines)
            val viewModel =
                DiagnosticsViewModel(
                    secureSessionAccessState = FullAccess,
                    logStore = store,
                    exportDiagnostics = ExportDiagnosticsUseCase(FullAccess, UnusedAuthorizationGate, UnusedExportGateway),
                    logger = UnusedLogger,
                    clipboardWriter = RecordingClipboardWriter(),
                )

            viewModel.onPageEntered()
            runCurrent()

            assertEquals(1, store.readCount)
            assertEquals(lines, viewModel.uiState.value.retainedLines)
        }

    @Test
    fun `failed initial read is visible and retry recovers`() =
        runTest {
            val lines = listOf(DiagnosticsLogLine(LogLevel.INFO, "recovered", "trace_ok", "application.ok"))
            val store = ScriptedReadLogStore(lines, failures = mapOf(1 to IOException("read failed")))
            val viewModel = viewModel(store)

            viewModel.onPageEntered()
            runCurrent()

            assertFalse(viewModel.uiState.value.loadState is DiagnosticsLoadState.Loading)
            assertTrue(viewModel.uiState.value.loadState is DiagnosticsLoadState.RefreshFailed)
            assertTrue(
                viewModel.uiState.value.retainedLines
                    .isEmpty(),
            )

            viewModel.onAction(DiagnosticsAction.Refresh)
            runCurrent()

            assertFalse(viewModel.uiState.value.loadState is DiagnosticsLoadState.Loading)
            assertFalse(viewModel.uiState.value.loadState is DiagnosticsLoadState.RefreshFailed)
            assertEquals(lines, viewModel.uiState.value.retainedLines)
        }

    @Test
    fun `failed refresh preserves previously loaded lines`() =
        runTest {
            val lines = listOf(DiagnosticsLogLine(LogLevel.INFO, "existing", "trace_old", "application.old"))
            val store = ScriptedReadLogStore(lines, failures = mapOf(2 to IOException("refresh failed")))
            val viewModel = viewModel(store)
            viewModel.onPageEntered()
            runCurrent()

            viewModel.onAction(DiagnosticsAction.Refresh)
            runCurrent()

            assertFalse(viewModel.uiState.value.loadState is DiagnosticsLoadState.Loading)
            assertTrue(viewModel.uiState.value.loadState is DiagnosticsLoadState.RefreshFailed)
            assertEquals(lines, viewModel.uiState.value.retainedLines)
        }

    @Test
    fun `cancelled and fatal reads are not converted to refresh failures`() {
        val fatal = AssertionError("fatal read")
        val thrown =
            runCatching {
                runTest {
                    val cancelledStore =
                        ScriptedReadLogStore(
                            emptyList(),
                            failures = mapOf(1 to CancellationException("cancelled")),
                        )
                    val cancelledViewModel = viewModel(cancelledStore)
                    cancelledViewModel.onPageEntered()
                    runCurrent()
                    assertFalse(cancelledViewModel.uiState.value.loadState is DiagnosticsLoadState.RefreshFailed)

                    val fatalViewModel = viewModel(ScriptedReadLogStore(emptyList(), failures = mapOf(1 to fatal)))
                    fatalViewModel.onPageEntered()
                    runCurrent()

                    assertFalse(fatalViewModel.uiState.value.loadState is DiagnosticsLoadState.RefreshFailed)
                }
            }.exceptionOrNull()

        assertSame(fatal, thrown)
    }

    @Test
    fun `failed clear reports failure and keeps retained lines`() =
        runTest {
            val lines =
                listOf(
                    DiagnosticsLogLine(LogLevel.INFO, "row-one", "trace_one", "application.one"),
                )
            val store = RecordingLogStore(lines, clearResult = LogClearResult.Failed)
            val viewModel = viewModel(store)
            viewModel.onPageEntered()
            runCurrent()
            val effect = async { viewModel.effects.first() }

            viewModel.onAction(DiagnosticsAction.RequestClear)
            viewModel.onAction(DiagnosticsAction.ConfirmClear)
            runCurrent()

            assertEquals(DiagnosticsEffect.ClearFailed, effect.await())
            assertEquals(lines, viewModel.uiState.value.retainedLines)
            assertFalse(viewModel.uiState.value.clearState.isConfirmationVisible)
        }

    @Test
    fun `duplicate clear confirmation starts only one storage operation`() =
        runTest {
            val store = DeferredClearLogStore(emptyList())
            val viewModel = viewModel(store)
            viewModel.onPageEntered()
            runCurrent()

            viewModel.onAction(DiagnosticsAction.RequestClear)
            viewModel.onAction(DiagnosticsAction.ConfirmClear)
            viewModel.onAction(DiagnosticsAction.ConfirmClear)
            runCurrent()
            store.clearStarted.await()

            assertEquals(1, store.clearCount)
            assertTrue(viewModel.uiState.value.clearState.isClearing)

            store.finishClear.complete(LogClearResult.Cleared)
            runCurrent()
            assertFalse(viewModel.uiState.value.clearState.isClearing)
        }

    @Test
    fun `late clear failure cannot affect a reentered page`() =
        runTest {
            val oldLines = listOf(DiagnosticsLogLine(LogLevel.INFO, "old", "trace_old", "application.old"))
            val newLines = listOf(DiagnosticsLogLine(LogLevel.INFO, "new", "trace_new", "application.new"))
            val store = DeferredClearLogStore(oldLines, reenteredLines = newLines)
            val viewModel = viewModel(store)
            val oldOwner = viewModel.onPageEntered()
            runCurrent()

            viewModel.onAction(DiagnosticsAction.RequestClear)
            viewModel.onAction(DiagnosticsAction.ConfirmClear)
            runCurrent()
            store.clearStarted.await()

            viewModel.onPageExited(oldOwner)
            viewModel.onPageEntered()
            runCurrent()
            viewModel.onAction(DiagnosticsAction.RequestClear)
            viewModel.onAction(DiagnosticsAction.ConfirmClear)
            runCurrent()

            assertTrue(viewModel.uiState.value.clearState.isClearing)
            assertEquals(1, store.clearCount)
            store.finishClear.complete(LogClearResult.Failed)
            runCurrent()

            assertEquals(newLines, viewModel.uiState.value.retainedLines)
            assertTrue(viewModel.uiState.value.clearState.isConfirmationVisible)
            assertFalse(viewModel.uiState.value.clearState.isClearing)
            assertNull(withTimeoutOrNull(1) { viewModel.effects.first() })
        }

    @Test
    fun `late successful clear empties lines loaded by a reentered page`() =
        runTest {
            val oldLines = listOf(DiagnosticsLogLine(LogLevel.INFO, "old", "trace_old", "application.old"))
            val newLines = listOf(DiagnosticsLogLine(LogLevel.INFO, "new", "trace_new", "application.new"))
            val store = DeferredClearLogStore(oldLines, reenteredLines = newLines)
            val viewModel = viewModel(store)
            val oldOwner = viewModel.onPageEntered()
            runCurrent()

            viewModel.onAction(DiagnosticsAction.RequestClear)
            viewModel.onAction(DiagnosticsAction.ConfirmClear)
            runCurrent()
            store.clearStarted.await()

            viewModel.onPageExited(oldOwner)
            viewModel.onPageEntered()
            runCurrent()
            store.finishClear.complete(LogClearResult.Cleared)
            runCurrent()

            assertTrue(viewModel.uiState.value.loadState is DiagnosticsLoadState.Ready)
            assertEquals(emptyList<DiagnosticsLogLine>(), viewModel.uiState.value.retainedLines)
        }

    @Test
    fun `copy row and trace use the sensitive clipboard writer with bounded rows`() =
        runTest {
            val traceLines =
                (0..100).map { index ->
                    DiagnosticsLogLine(LogLevel.INFO, "row-$index", "trace_shared", "application.row")
                }
            val clipboard = RecordingClipboardWriter()
            val viewModel =
                viewModel(
                    store = RecordingLogStore(traceLines),
                    clipboardWriter = clipboard,
                )
            viewModel.onPageEntered()
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
    fun `page exit prevents a suspended diagnostics copy from publishing`() =
        runTest {
            val clipboard = DelayedClipboardWriter()
            val lines = listOf(DiagnosticsLogLine(LogLevel.INFO, "row-one", "trace_one", "application.one"))
            val viewModel = viewModel(RecordingLogStore(lines), clipboardWriter = clipboard)
            val pageOwner = viewModel.onPageEntered()
            runCurrent()

            viewModel.onAction(DiagnosticsAction.CopyRow(lines.single()))
            runCurrent()
            clipboard.writeStarted.await()
            viewModel.onPageExited(pageOwner)
            clipboard.finishWrite.complete(Unit)
            runCurrent()

            assertEquals(emptyList<String>(), clipboard.values)
        }

    @Test
    fun `late log read cannot replace lines loaded after reentering diagnostics`() =
        runTest {
            val oldLines = listOf(DiagnosticsLogLine(LogLevel.INFO, "old", "trace_old", "application.old"))
            val newLines = listOf(DiagnosticsLogLine(LogLevel.INFO, "new", "trace_new", "application.new"))
            val store = DeferredReadLogStore()
            val viewModel = viewModel(store)

            val oldOwner = viewModel.onPageEntered()
            runCurrent()
            store.readStarted[0].await()

            viewModel.onPageExited(oldOwner)
            viewModel.onPageEntered()
            runCurrent()
            store.readStarted[1].await()

            store.completeRead(1, newLines)
            runCurrent()
            assertEquals(newLines, viewModel.uiState.value.retainedLines)

            store.completeRead(0, oldLines)
            runCurrent()

            assertEquals(newLines, viewModel.uiState.value.retainedLines)
        }

    @Test
    fun `reentry does not replay export feedback from the previous page visit`() =
        runTest {
            val gateway = DeferredExportGateway()
            val viewModel =
                viewModel(
                    store = RecordingLogStore(emptyList()),
                    gate = AllowingAuthorizationGate,
                    gateway = gateway,
                    logger = ExecutingLogger,
                )
            val oldOwner = viewModel.onPageEntered()
            runCurrent()

            val pickerEffect = async { viewModel.effects.first() }
            viewModel.onAction(DiagnosticsAction.RequestExport(ExportAction.EXPORT_LOCAL))
            viewModel.onAction(DiagnosticsAction.SelectFormat(LogFormat.JSONL))
            val picker = pickerEffect.await() as DiagnosticsEffect.SelectSaveDocument
            assertEquals(LogFormat.JSONL, picker.format)
            assertEquals("application/x-ndjson", picker.mimeType)
            assertEquals("passly_logs.jsonl", picker.fileName)

            viewModel.onAction(
                DiagnosticsAction.SaveToDocument("content://test/export", oldOwner, picker.requestId),
            )
            gateway.exportStarted.await()
            viewModel.onPageExited(oldOwner)
            viewModel.onPageEntered()
            gateway.exportResult.complete(Result.success(Unit))
            runCurrent()

            assertNull(withTimeoutOrNull(1) { viewModel.effects.first() })
        }

    @Test
    fun `local format selection emits matching saf contract independent of level filters`() =
        runTest {
            val viewModel = viewModel(RecordingLogStore(emptyList()))
            val owner = viewModel.onPageEntered()
            viewModel.onAction(DiagnosticsAction.ToggleLevel(LogLevel.INFO))
            viewModel.onAction(DiagnosticsAction.RequestExport(ExportAction.EXPORT_LOCAL))

            val effect = async { viewModel.effects.first() }
            viewModel.onAction(DiagnosticsAction.SelectFormat(LogFormat.TSV))

            val request = effect.await() as DiagnosticsEffect.SelectSaveDocument
            assertEquals(LogFormat.TSV, request.format)
            assertEquals("text/tab-separated-values", request.mimeType)
            assertEquals("passly_logs.tsv", request.fileName)
            assertEquals(
                DiagnosticsExportState.SelectingLocalDocument(LogFormat.TSV),
                viewModel.uiState.value.exportState,
            )

            viewModel.onAction(DiagnosticsAction.SaveDocumentCancelled(owner, request.requestId))

            assertEquals(DiagnosticsExportState.None, viewModel.uiState.value.exportState)
        }

    @Test
    fun `late document result from an exited page cannot start an export`() =
        runTest {
            val gateway = RecordingExportGateway()
            val viewModel =
                viewModel(
                    store = RecordingLogStore(emptyList()),
                    gate = AllowingAuthorizationGate,
                    gateway = gateway,
                    logger = ExecutingLogger,
                )
            val oldOwner = viewModel.onPageEntered()
            viewModel.onAction(DiagnosticsAction.RequestExport(ExportAction.EXPORT_LOCAL))
            val effect = async { viewModel.effects.first() }
            viewModel.onAction(DiagnosticsAction.SelectFormat(LogFormat.JSONL))
            val picker = effect.await() as DiagnosticsEffect.SelectSaveDocument

            viewModel.onPageExited(oldOwner)
            viewModel.onPageEntered()
            viewModel.onAction(
                DiagnosticsAction.SaveToDocument("content://stale", oldOwner, picker.requestId),
            )
            runCurrent()

            assertNull(gateway.request)
            assertEquals(DiagnosticsExportState.None, viewModel.uiState.value.exportState)
        }

    @Test
    fun `share format reaches authenticated gateway without viewer filters`() =
        runTest {
            val gateway = RecordingExportGateway()
            val viewModel =
                viewModel(
                    store = RecordingLogStore(emptyList()),
                    gate = AllowingAuthorizationGate,
                    gateway = gateway,
                    logger = ExecutingLogger,
                )
            viewModel.onPageEntered()
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

    private class ScriptedReadLogStore(
        private val lines: List<DiagnosticsLogLine>,
        private val failures: Map<Int, Throwable>,
    ) : DiagnosticsLogStore {
        var readCount = 0

        override suspend fun readLines(limit: Int): List<DiagnosticsLogLine> {
            readCount++
            failures[readCount]?.let { throw it }
            return lines.take(limit)
        }

        override suspend fun storageBytes(): Long = 0

        override suspend fun clear() = LogClearResult.Cleared
    }

    private class DeferredClearLogStore(
        private val initialLines: List<DiagnosticsLogLine>,
        private val reenteredLines: List<DiagnosticsLogLine> = initialLines,
    ) : DiagnosticsLogStore {
        val clearStarted = CompletableDeferred<Unit>()
        val finishClear = CompletableDeferred<LogClearResult>()
        var clearCount = 0
        private var readCount = 0

        override suspend fun readLines(limit: Int): List<DiagnosticsLogLine> =
            (if (readCount++ == 0) initialLines else reenteredLines).take(limit)

        override suspend fun storageBytes(): Long = 0

        override suspend fun clear(): LogClearResult {
            clearCount++
            clearStarted.complete(Unit)
            return finishClear.await()
        }
    }

    private class DeferredReadLogStore : DiagnosticsLogStore {
        val readStarted = mutableListOf<CompletableDeferred<Unit>>()
        private val readContinuations = mutableListOf<Continuation<List<DiagnosticsLogLine>>>()

        override suspend fun readLines(limit: Int): List<DiagnosticsLogLine> =
            suspendCoroutine { continuation ->
                readContinuations += continuation
                readStarted += CompletableDeferred<Unit>().also { it.complete(Unit) }
            }

        fun completeRead(
            index: Int,
            lines: List<DiagnosticsLogLine>,
        ) {
            readContinuations[index].resume(lines)
        }

        override suspend fun storageBytes(): Long = 0

        override suspend fun clear() = LogClearResult.Cleared
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

        override suspend fun writeSensitiveIf(
            text: String,
            canWrite: () -> Boolean,
        ): Boolean =
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

    private class DeferredExportGateway : DiagnosticsExportGateway {
        val exportStarted = CompletableDeferred<Unit>()
        val exportResult = CompletableDeferred<Result<Unit>>()

        override suspend fun export(
            permit: AuthorizationPermit,
            request: DiagnosticsExportRequest,
        ): Result<Unit> {
            exportStarted.complete(Unit)
            return exportResult.await()
        }

        override suspend fun discard(request: DiagnosticsExportRequest) = Unit
    }

    private object UnusedLogger : Logger {
        override fun debug(
            event: EventKey,
            fields: LogFields,
        ) = error("Logger is not used")

        override fun info(
            event: EventKey,
            fields: LogFields,
        ) = error("Logger is not used")

        override fun degraded(
            event: EventKey,
            errorCode: ErrorCode,
            reason: ReasonCode,
            fields: LogFields,
        ) = error("Logger is not used")

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
