package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.port.AuthenticationRequester
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.access.port.SessionLockController
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.domain.autofill.model.AutofillInteractionState
import com.aozijx.passly.feature.autofill.core.StartAutofillInteraction
import com.aozijx.passly.feature.autofill.runtime.AutofillInteractionStore
import com.aozijx.passly.feature.autofill.runtime.AutofillUnlockLease
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** Security boundary tests for the limited Recovery Mode session. */
class RecoveryModeBoundaryTest {
    @Test
    fun `recovery mode requires authorization before autofill`() = runBlocking {
        val sessionState = RecoverySecureSessionAccessState()
        val interaction = StartAutofillInteraction(
            store = AutofillInteractionStore(),
            unlockLease = AutofillUnlockLease(NoOpAuthenticationController),
            vaultAccessState = sessionState,
        )(
            source = AutofillInteractionSource.LEGACY,
            scope = AutofillGrantContext("com.example"),
            requireAuthentication = false,
        )

        assertEquals(AutofillInteractionState.RequiresUnlock, interaction.state)
    }

    @Test
    fun `recovery mode hasFullSecureSessionAccess returns false`() {
        val state = RecoverySecureSessionAccessState()
        assertEquals(true, state.isDatabaseOpen())
        assertEquals(false, state.hasFullSecureSessionAccess())
        assertEquals(true, state.isRecoveryMode())
    }

    @Test
    fun `authenticated mode hasFullSecureSessionAccess returns true`() {
        val state = AuthenticatedSecureSessionAccessState()
        assertEquals(true, state.isDatabaseOpen())
        assertEquals(true, state.hasFullSecureSessionAccess())
        assertEquals(false, state.isRecoveryMode())
    }

    @Test
    fun `locked mode hasFullSecureSessionAccess returns false`() {
        val state = LockedSecureSessionAccessState()
        assertEquals(false, state.isDatabaseOpen())
        assertEquals(false, state.hasFullSecureSessionAccess())
        assertEquals(false, state.isRecoveryMode())
    }

    private class RecoverySecureSessionAccessState : SecureSessionAccessState {
        override val authenticationState = MutableStateFlow<AuthenticationState>(
            AuthenticationState.RecoveryMode(authenticatedAtMs = 0L),
        )
        override fun isUnlocked(): Boolean = true
    }

    private class AuthenticatedSecureSessionAccessState : SecureSessionAccessState {
        override val authenticationState = MutableStateFlow<AuthenticationState>(
            AuthenticationState.Authenticated(authenticatedAtMs = 0L),
        )
        override fun isUnlocked(): Boolean = true
    }

    private class LockedSecureSessionAccessState : SecureSessionAccessState {
        override val authenticationState = MutableStateFlow<AuthenticationState>(
            AuthenticationState.Locked,
        )
        override fun isUnlocked(): Boolean = false
    }

    private object NoOpAuthenticationController : AuthenticationRequester, SessionLockController {
        override suspend fun authenticate(
            request: AuthenticationRequest,
            input: AuthInput,
        ): AuthenticationResult = error("Recovery-mode Autofill must not authenticate during discovery")

        override suspend fun lock(reason: LockReason) = Unit
    }
}
