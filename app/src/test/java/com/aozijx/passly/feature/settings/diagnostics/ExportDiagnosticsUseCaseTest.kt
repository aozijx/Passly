package com.aozijx.passly.feature.settings.diagnostics

import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportDiagnosticsUseCaseTest {
    @Test
    fun `all action and format pairs reach the gateway unchanged inside authorization`() = runTest {
        ExportAction.entries.forEach { action ->
            LogFormat.entries.forEach { format ->
                val gate = RecordingGate(true)
                val gateway = RecordingGateway(gate)
                val request = DiagnosticsExportRequest(
                    action = action,
                    format = format,
                    documentUri = if (action == ExportAction.EXPORT_LOCAL) "content://documents/log.${format.extension}" else null,
                )

                val result = ExportDiagnosticsUseCase(FullAccess, gate, gateway)(request)

                assertEquals(DiagnosticsExportResult.Completed, result)
                assertEquals(AuthorizationScope.Global(AuthenticationPurpose.EXPORT_DIAGNOSTICS), gate.scope)
                assertEquals(true, gateway.calledInsideAuthorization)
                assertEquals(request, gateway.request)
            }
        }
    }

    @Test
    fun `document export keeps selected destination inside authorization scope`() = runTest {
        val gate = RecordingGate(true)
        val gateway = RecordingGateway(gate)
        val request = DiagnosticsExportRequest(
            ExportAction.EXPORT_LOCAL,
            LogFormat.TSV,
            "content://documents/log.tsv",
        )

        val result = ExportDiagnosticsUseCase(FullAccess, gate, gateway)(request)

        assertEquals(DiagnosticsExportResult.Completed, result)
        assertEquals(request, gateway.request)
        assertEquals(true, gateway.calledInsideAuthorization)
    }

    @Test
    fun `cancelled authorization never creates plaintext export`() = runTest {
        val gate = RecordingGate(false)
        val gateway = RecordingGateway(gate)
        val request = DiagnosticsExportRequest(ExportAction.EXPORT_LOCAL, LogFormat.ZIP, "content://documents/cancelled.zip")
        assertEquals(
            DiagnosticsExportResult.Cancelled,
            ExportDiagnosticsUseCase(FullAccess, gate, gateway)(request),
        )
        assertEquals(0, gateway.calls)
        assertEquals(request, gateway.discardedRequest)
    }

    @Test
    fun `failed document export discards picker-created document`() = runTest {
        val gate = RecordingGate(true)
        val gateway = RecordingGateway(
            gate = gate,
            result = Result.failure(IllegalStateException("write failed")),
        )
        val request = DiagnosticsExportRequest(ExportAction.EXPORT_LOCAL, LogFormat.JSONL, "content://documents/incomplete.jsonl")

        val result = ExportDiagnosticsUseCase(FullAccess, gate, gateway)(request)

        assertEquals(true, result is DiagnosticsExportResult.Failed)
        assertEquals(request, gateway.discardedRequest)
    }

    private class RecordingGate(private val allow: Boolean) : AuthorizationGate {
        var scope: AuthorizationScope? = null
        var inside = false
        override suspend fun <T> authorize(scope: AuthorizationScope, input: AuthInput, block: suspend (AuthorizationPermit) -> T): AuthorizationResult<T> {
            this.scope = scope
            if (!allow) return AuthorizationResult.Cancelled
            inside = true
            return try { AuthorizationResult.Allowed(block(object : AuthorizationPermit {})) } finally { inside = false }
        }
    }

    private class RecordingGateway(
        private val gate: RecordingGate,
        private val result: Result<Unit> = Result.success(Unit),
    ) : DiagnosticsExportGateway {
        var calls = 0
        var calledInsideAuthorization = false
        var request: DiagnosticsExportRequest? = null
        var discardedRequest: DiagnosticsExportRequest? = null
        override suspend fun export(
            permit: AuthorizationPermit,
            request: DiagnosticsExportRequest,
        ): Result<Unit> {
            calls++
            calledInsideAuthorization = gate.inside
            this.request = request
            return result
        }

        override suspend fun discard(request: DiagnosticsExportRequest) {
            discardedRequest = request
        }
    }

    private object FullAccess : SecureSessionAccessState {
        override val authenticationState: StateFlow<AuthenticationState> = MutableStateFlow(AuthenticationState.Authenticated(1L))
        override fun isUnlocked() = true
    }
}
