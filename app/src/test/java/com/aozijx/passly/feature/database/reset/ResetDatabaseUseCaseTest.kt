package com.aozijx.passly.feature.database.reset

import com.aozijx.passly.core.error.result.AppResult
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResetDatabaseUseCaseTest {
    @Test
    fun `reset executes inside exact clear database authorization scope`() = runTest {
        val gateway = RecordingGateway()
        val gate = RecordingGate(AuthorizationDecision.ALLOW)
        gateway.gate = gate
        val useCase = ResetDatabaseUseCase(FullAccess, gate, gateway)

        val result = useCase()

        assertEquals(DatabaseResetResult.Completed, result)
        assertEquals(AuthorizationScope.Global(AuthenticationPurpose.CLEAR_DATABASE), gate.scope)
        assertTrue(gateway.calledInsideAuthorization)
        assertEquals(1, gateway.resetCount)
    }

    @Test
    fun `cancelled authorization does not reset database`() = runTest {
        val gateway = RecordingGateway()
        val useCase = ResetDatabaseUseCase(
            FullAccess,
            RecordingGate(AuthorizationDecision.CANCEL),
            gateway,
        )

        assertEquals(DatabaseResetResult.Cancelled, useCase())
        assertEquals(0, gateway.resetCount)
    }

    @Test
    fun `restricted session is rejected before authorization`() = runTest {
        val gate = RecordingGate(AuthorizationDecision.ALLOW)
        val gateway = RecordingGateway()
        val useCase = ResetDatabaseUseCase(RestrictedAccess, gate, gateway)

        assertEquals(DatabaseResetResult.SessionRestricted, useCase())
        assertFalse(gate.authorizeCalled)
        assertEquals(0, gateway.resetCount)
    }

    private enum class AuthorizationDecision { ALLOW, CANCEL }

    private class RecordingGate(
        private val decision: AuthorizationDecision,
    ) : AuthorizationGate {
        var scope: AuthorizationScope? = null
        var authorizeCalled = false
        var insideAuthorization = false

        override suspend fun <T> authorize(
            scope: AuthorizationScope,
            input: AuthInput,
            block: suspend (AuthorizationPermit) -> T,
        ): AuthorizationResult<T> {
            authorizeCalled = true
            this.scope = scope
            if (decision == AuthorizationDecision.CANCEL) return AuthorizationResult.Cancelled
            insideAuthorization = true
            return try {
                AuthorizationResult.Allowed(block(object : AuthorizationPermit {}))
            } finally {
                insideAuthorization = false
            }
        }
    }

    private class RecordingGateway : DatabaseResetGateway {
        var resetCount = 0
        var calledInsideAuthorization = false
        lateinit var gate: RecordingGate

        override suspend fun reset(): AppResult<Unit> {
            resetCount++
            calledInsideAuthorization = gate.insideAuthorization
            return AppResult.Success(Unit)
        }
    }

    private object FullAccess : SecureSessionAccessState {
        override val authenticationState: StateFlow<AuthenticationState> =
            MutableStateFlow(AuthenticationState.Authenticated(1L))
        override fun isUnlocked() = true
    }

    private object RestrictedAccess : SecureSessionAccessState {
        override val authenticationState: StateFlow<AuthenticationState> =
            MutableStateFlow(AuthenticationState.RecoveryMode(1L))
        override fun isUnlocked() = true
    }
}