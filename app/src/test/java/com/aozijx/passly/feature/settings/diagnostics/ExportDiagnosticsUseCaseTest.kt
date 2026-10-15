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
    fun `share executes inside exact diagnostics authorization scope`() = runTest {
        val gate = RecordingGate(true)
        val gateway = RecordingGateway(gate)
        val result = ExportDiagnosticsUseCase(FullAccess, gate, gateway)(
            DiagnosticsExportTarget.Share,
        )
        assertEquals(DiagnosticsExportResult.Completed, result)
        assertEquals(AuthorizationScope.Global(AuthenticationPurpose.EXPORT_DIAGNOSTICS), gate.scope)
        assertEquals(true, gateway.calledInsideAuthorization)
        assertEquals(1, gateway.calls)
        assertEquals(DiagnosticsExportTarget.Share, gateway.target)
    }

    @Test
    fun `document export keeps selected destination inside authorization scope`() = runTest {
        val gate = RecordingGate(true)
        val gateway = RecordingGateway(gate)
        val target = DiagnosticsExportTarget.Document("content://documents/log.txt")

        val result = ExportDiagnosticsUseCase(FullAccess, gate, gateway)(target)

        assertEquals(DiagnosticsExportResult.Completed, result)
        assertEquals(target, gateway.target)
        assertEquals(true, gateway.calledInsideAuthorization)
    }

    @Test
    fun `cancelled authorization never creates plaintext export`() = runTest {
        val gate = RecordingGate(false)
        val gateway = RecordingGateway(gate)
        val target = DiagnosticsExportTarget.Document("content://documents/cancelled.txt")
        assertEquals(
            DiagnosticsExportResult.Cancelled,
            ExportDiagnosticsUseCase(FullAccess, gate, gateway)(target),
        )
        assertEquals(0, gateway.calls)
        assertEquals(target, gateway.discardedTarget)
    }

    @Test
    fun `failed document export discards picker-created document`() = runTest {
        val gate = RecordingGate(true)
        val gateway = RecordingGateway(
            gate = gate,
            result = Result.failure(IllegalStateException("write failed")),
        )
        val target = DiagnosticsExportTarget.Document("content://documents/incomplete.txt")

        val result = ExportDiagnosticsUseCase(FullAccess, gate, gateway)(target)

        assertEquals(true, result is DiagnosticsExportResult.Failed)
        assertEquals(target, gateway.discardedTarget)
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
        var target: DiagnosticsExportTarget? = null
        var discardedTarget: DiagnosticsExportTarget? = null
        override suspend fun export(
            permit: AuthorizationPermit,
            target: DiagnosticsExportTarget,
        ): Result<Unit> {
            calls++
            calledInsideAuthorization = gate.inside
            this.target = target
            return result
        }

        override suspend fun discard(target: DiagnosticsExportTarget) {
            discardedTarget = target
        }
    }

    private object FullAccess : SecureSessionAccessState {
        override val authenticationState: StateFlow<AuthenticationState> = MutableStateFlow(AuthenticationState.Authenticated(1L))
        override fun isUnlocked() = true
    }
}
