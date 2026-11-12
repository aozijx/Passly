package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

/** Security boundary tests for the limited Recovery Mode session. */
class RecoveryModeBoundaryTest {
    @Test
    fun `recovery mode hasFullSecureSessionAccess returns false`() {
        val state = RecoverySecureSessionAccessState()
        assertEquals(true, state.isUnlocked())
        assertEquals(false, state.hasFullSecureSessionAccess())
        assertEquals(true, state.isRecoveryMode())
    }

    @Test
    fun `authenticated mode hasFullSecureSessionAccess returns true`() {
        val state = AuthenticatedSecureSessionAccessState()
        assertEquals(true, state.isUnlocked())
        assertEquals(true, state.hasFullSecureSessionAccess())
        assertEquals(true, state.hasAutofillSessionAccess())
        assertEquals(false, state.isRecoveryMode())
    }

    @Test
    fun `autofill session opens storage without granting full app access`() {
        val state = AutofillSecureSessionAccessState()
        assertEquals(true, state.isUnlocked())
        assertEquals(false, state.hasFullSecureSessionAccess())
        assertEquals(true, state.hasAutofillSessionAccess())
    }

    @Test
    fun `locked mode hasFullSecureSessionAccess returns false`() {
        val state = LockedSecureSessionAccessState()
        assertEquals(false, state.isUnlocked())
        assertEquals(false, state.hasFullSecureSessionAccess())
        assertEquals(false, state.isRecoveryMode())
    }

    @Test
    fun `locking storage is not exposed as full session access`() {
        val state = LockingSecureSessionAccessState()
        assertEquals(true, state.isUnlocked())
        assertEquals(false, state.hasFullSecureSessionAccess())
    }

    private class RecoverySecureSessionAccessState : SecureSessionAccessState {
        override val authenticationState =
            MutableStateFlow<AuthenticationState>(
                AuthenticationState.RecoveryMode(authenticatedAtMs = 0L),
            )

        override fun isUnlocked(): Boolean = true
    }

    private class AuthenticatedSecureSessionAccessState : SecureSessionAccessState {
        override val authenticationState =
            MutableStateFlow<AuthenticationState>(
                AuthenticationState.Authenticated(authenticatedAtMs = 0L),
            )

        override fun isUnlocked(): Boolean = true
    }

    private class AutofillSecureSessionAccessState : SecureSessionAccessState {
        override val authenticationState =
            MutableStateFlow<AuthenticationState>(
                AuthenticationState.AutofillAuthenticated(authenticatedAtMs = 0L),
            )

        override fun isUnlocked(): Boolean = true
    }

    private class LockedSecureSessionAccessState : SecureSessionAccessState {
        override val authenticationState =
            MutableStateFlow<AuthenticationState>(
                AuthenticationState.Locked,
            )

        override fun isUnlocked(): Boolean = false
    }

    private class LockingSecureSessionAccessState : SecureSessionAccessState {
        override val authenticationState =
            MutableStateFlow<AuthenticationState>(
                AuthenticationState.Locking(LockReason.AUTOFILL_REQUEST_FINISHED),
            )

        override fun isUnlocked(): Boolean = true
    }
}
