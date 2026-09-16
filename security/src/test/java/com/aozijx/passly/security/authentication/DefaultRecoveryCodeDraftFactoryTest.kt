package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.model.RecoveryCredentialCreation
import com.aozijx.passly.domain.access.model.RecoveryCredentialDraft
import com.aozijx.passly.domain.access.model.RecoveryCredentialId
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultRecoveryCodeDraftFactoryTest {
    @Test
    fun `creation runs inside exact recovery authorization scope`() = runBlocking {
        val session = FakeSession(AuthenticationState.Authenticated(1L))
        val gate = RecordingGate()
        val draft = FakeDraft()
        val generator = RecordingGenerator(gate) { RecoveryCredentialCreation.Ready(draft) }

        val result = DefaultRecoveryCodeDraftFactory(gate, session, generator).create()

        assertEquals(
            AuthorizationScope.Global(AuthenticationPurpose.MANAGE_RECOVERY_CODE),
            gate.scope,
        )
        assertTrue(generator.ranInsideAuthorization)
        assertTrue(result is RecoveryCredentialCreation.Ready)
    }

    @Test
    fun `cancelled authorization does not generate a recovery draft`() = runBlocking {
        val session = FakeSession(AuthenticationState.Authenticated(1L))
        val gate = RecordingGate(cancel = true)
        val generator = RecordingGenerator(gate) { error("must not generate") }

        val result = DefaultRecoveryCodeDraftFactory(gate, session, generator).create()

        assertEquals(RecoveryCredentialCreation.Cancelled, result)
        assertEquals(0, generator.callCount)
    }

    @Test
    fun `restricted session is rejected before authorization`() = runBlocking {
        val session = FakeSession(AuthenticationState.RecoveryMode(1L))
        val gate = RecordingGate()
        val generator = RecordingGenerator(gate) { error("must not generate") }

        val result = DefaultRecoveryCodeDraftFactory(gate, session, generator).create()

        assertTrue(result is RecoveryCredentialCreation.Failed)
        assertNull(gate.scope)
        assertEquals(0, generator.callCount)
    }

    @Test
    fun `session bound draft refuses reveal and commit after session expires`() = runBlocking {
        val session = FakeSession(AuthenticationState.Authenticated(1L))
        val delegate = FakeDraft()
        val draft = SessionBoundRecoveryCredentialDraft(
            delegate,
            session,
            session.state.value as AuthenticationState.Authenticated,
        )

        assertEquals("RECOVERY", draft.reveal()?.concatToString())
        session.state.value = AuthenticationState.Locked

        assertNull(draft.reveal())
        val result = draft.commit()
        assertTrue(result is AuthenticationResult.Failure)
        assertFalse(delegate.committed)
    }

    private class RecordingGate(
        private val cancel: Boolean = false,
    ) : AuthorizationGate {
        var scope: AuthorizationScope? = null
        var insideAuthorization = false

        override suspend fun <T> authorize(
            scope: AuthorizationScope,
            input: com.aozijx.passly.domain.access.model.AuthInput,
            block: suspend (AuthorizationPermit) -> T,
        ): AuthorizationResult<T> {
            this.scope = scope
            if (cancel) return AuthorizationResult.Cancelled
            insideAuthorization = true
            return try {
                AuthorizationResult.Allowed(block(object : AuthorizationPermit {}))
            } finally {
                insideAuthorization = false
            }
        }
    }

    private class RecordingGenerator(
        private val gate: RecordingGate,
        private val result: () -> RecoveryCredentialCreation,
    ) : RecoveryCredentialGenerator {
        var callCount = 0
        var ranInsideAuthorization = false

        override suspend fun generate(): RecoveryCredentialCreation {
            callCount++
            ranInsideAuthorization = gate.insideAuthorization
            return result()
        }
    }

    private class FakeSession(initial: AuthenticationState) : SecureSessionAccessState {
        val state = MutableStateFlow(initial)
        override val authenticationState = state
        override fun isUnlocked(): Boolean = state.value is AuthenticationState.Authenticated ||
            state.value is AuthenticationState.RecoveryMode
    }

    private class FakeDraft : RecoveryCredentialDraft {
        override val id = RecoveryCredentialId("draft")
        var committed = false
        override fun reveal(): CharArray = "RECOVERY".toCharArray()
        override suspend fun commit(): AuthenticationResult {
            committed = true
            return AuthenticationResult.Success(com.aozijx.passly.domain.access.model.AuthenticationMethod.RECOVERY_CODE)
        }
        override fun close() = Unit
    }
}
