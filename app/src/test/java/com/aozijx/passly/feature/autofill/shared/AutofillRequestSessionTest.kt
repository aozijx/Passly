package com.aozijx.passly.feature.autofill.shared

import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationMethods
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationSnapshot
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutofillRequestSessionTest {

    @Test
    fun `request relocks only the vault unlock it acquired`() = runBlocking {
        val vault = FakeSecureSessionAccessState(unlocked = false)
        val authentication = FakeAuthenticationManager(vault)
        val session = AutofillRequestSession(authentication, vault)

        assertEquals(AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD), session.authenticate())
        session.close()

        assertEquals(LockReason.AUTOFILL_REQUEST_FINISHED, authentication.lastLockReason)
    }

    @Test
    fun `request does not lock a vault that was already unlocked`() = runBlocking {
        val vault = FakeSecureSessionAccessState(unlocked = true)
        val authentication = FakeAuthenticationManager(vault)
        val session = AutofillRequestSession(authentication, vault)

        session.trackUnlock { "result" }
        session.close()

        assertNull(authentication.lastLockReason)
    }

    private class FakeSecureSessionAccessState(
        unlocked: Boolean,
    ) : SecureSessionAccessState {
        var unlocked = unlocked
        private val stateFlow = MutableStateFlow<AuthenticationState>(
            if (unlocked) AuthenticationState.Authenticated(1L) else AuthenticationState.Locked
        )

        override val authenticationState: StateFlow<AuthenticationState> = stateFlow
        override fun isUnlocked(): Boolean = unlocked
    }

    private class FakeAuthenticationManager(
        private val vault: FakeSecureSessionAccessState,
    ) : AuthenticationManager {
        override val state = MutableStateFlow<AuthenticationState>(AuthenticationState.Locked)
        override val methods = MutableStateFlow(
            AuthenticationMethods(setOf(AuthenticationMethod.APP_PASSWORD))
        )
        var lastLockReason: LockReason? = null

        override suspend fun authenticate(
            request: AuthenticationRequest,
            input: AuthInput,
        ): AuthenticationResult {
            vault.unlocked = true
            return AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD)
        }

        override suspend fun lock(reason: LockReason) {
            lastLockReason = reason
            vault.unlocked = false
        }

        override suspend fun completeDatabaseRecovery(): Boolean = false
        override suspend fun refreshAvailability() = Unit
        override fun snapshot() = AuthenticationSnapshot(state.value, methods.value)
    }
}
