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
    fun `export and share execute inside exact diagnostics authorization scope`() = runTest {
        val gate = RecordingGate(true)
        val gateway = RecordingGateway(gate)
        val result = ExportDiagnosticsUseCase(FullAccess, gate, gateway)()
        assertEquals(DiagnosticsExportResult.Completed, result)
        assertEquals(AuthorizationScope.Global(AuthenticationPurpose.EXPORT_DIAGNOSTICS), gate.scope)
        assertEquals(true, gateway.calledInsideAuthorization)
        assertEquals(1, gateway.calls)
    }

    @Test
    fun `cancelled authorization never creates plaintext export`() = runTest {
        val gate = RecordingGate(false)
        val gateway = RecordingGateway(gate)
        assertEquals(DiagnosticsExportResult.Cancelled, ExportDiagnosticsUseCase(FullAccess, gate, gateway)())
        assertEquals(0, gateway.calls)
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

    private class RecordingGateway(private val gate: RecordingGate) : DiagnosticsExportGateway {
        var calls = 0
        var calledInsideAuthorization = false
        override suspend fun exportAndShare(): Result<Unit> {
            calls++
            calledInsideAuthorization = gate.inside
            return Result.success(Unit)
        }
    }

    private object FullAccess : SecureSessionAccessState {
        override val authenticationState: StateFlow<AuthenticationState> = MutableStateFlow(AuthenticationState.Authenticated(1L))
        override fun isUnlocked() = true
    }
}