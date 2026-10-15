package com.aozijx.passly.presentation.feature.settings.main.general

import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.core.observability.ErrorCode
import com.aozijx.passly.core.observability.EventKey
import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.TraceResumeResult
import com.aozijx.passly.core.observability.TraceScope
import com.aozijx.passly.core.observability.trace.TraceToken
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsExportGateway
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsExportTarget
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogStore
import com.aozijx.passly.feature.settings.diagnostics.ExportDiagnosticsUseCase
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiagnosticsSettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `refresh loads log cache and size while viewer remains closed`() = runTest {
        val store = RecordingLogStore(listOf("first", "second"))
        val viewModel = viewModel(store)

        viewModel.onAction(DiagnosticsSettingsAction.RefreshLogs)
        runCurrent()

        assertEquals(1, store.readCount)
        assertEquals("first\nsecond".toByteArray(Charsets.UTF_8).size, viewModel.uiState.value.logByteCount)
        assertSame(DiagnosticsOverlayUiState.Closed, viewModel.uiState.value.overlay)
    }

    @Test
    fun `opening viewer reuses refreshed cache without reading store again`() = runTest {
        val store = RecordingLogStore(listOf("cached line"))
        val viewModel = viewModel(store)
        viewModel.onAction(DiagnosticsSettingsAction.RefreshLogs)
        runCurrent()
        assertEquals(1, store.readCount)

        viewModel.onAction(DiagnosticsSettingsAction.OpenViewer)
        runCurrent()

        assertEquals(1, store.readCount)
        assertEquals(
            DiagnosticsLogPageUiState.Content("cached line"),
            (viewModel.uiState.value.overlay as DiagnosticsOverlayUiState.Viewer).page,
        )
    }

    private fun viewModel(store: DiagnosticsLogStore) = DiagnosticsSettingsViewModel(
        secureSessionAccessState = FullAccess,
        logStore = store,
        exportDiagnostics = ExportDiagnosticsUseCase(
            secureSessionAccessState = FullAccess,
            authorizationGate = UnusedAuthorizationGate,
            gateway = UnusedExportGateway,
        ),
        logger = UnusedLogger,
    )

    private class RecordingLogStore(private val lines: List<String>) : DiagnosticsLogStore {
        var readCount = 0

        override suspend fun readLines(limit: Int): List<String> {
            readCount++
            return lines.take(limit)
        }

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

    private object UnusedExportGateway : DiagnosticsExportGateway {
        override suspend fun export(
            permit: AuthorizationPermit,
            target: DiagnosticsExportTarget,
        ): Result<Unit> =
            error("Export is not used")

        override suspend fun discard(target: DiagnosticsExportTarget) = Unit
    }

    private object UnusedLogger : Logger {
        override fun debug(event: EventKey, fields: LogFields) = error("Logger is not used")
        override fun info(event: EventKey, fields: LogFields) = error("Logger is not used")
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
}
