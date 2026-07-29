package com.aozijx.passly.feature.autofill

import com.aozijx.passly.domain.authentication.AuthMethodAvailability
import com.aozijx.passly.domain.authentication.AuthenticationCallback
import com.aozijx.passly.domain.authentication.AuthenticationManager
import com.aozijx.passly.domain.authentication.AuthenticationMethod
import com.aozijx.passly.domain.authentication.AuthenticationRequest
import com.aozijx.passly.domain.authentication.AuthenticationRequestHandle
import com.aozijx.passly.domain.authentication.AuthenticationResult
import com.aozijx.passly.domain.authentication.AuthenticationSnapshot
import com.aozijx.passly.domain.authentication.AuthenticationState
import com.aozijx.passly.domain.authentication.LockReason
import com.aozijx.passly.domain.authentication.VaultAccessState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutofillRequestSessionTest {

    @Test
    fun `request relocks only the vault unlock it acquired`() = runBlocking {
        val vault = FakeVaultAccessState(unlocked = false)
        val authentication = FakeAuthenticationManager(vault)
        val session = AutofillRequestSession(authentication, vault)

        assertEquals(AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD), session.authenticate())
        session.close()

        assertEquals(LockReason.AUTOFILL_REQUEST_FINISHED, authentication.lastLockReason)
    }

    @Test
    fun `request does not lock a vault that was already unlocked`() = runBlocking {
        val vault = FakeVaultAccessState(unlocked = true)
        val authentication = FakeAuthenticationManager(vault)
        val session = AutofillRequestSession(authentication, vault)

        session.trackUnlock { "result" }
        session.close()

        assertNull(authentication.lastLockReason)
    }

    private class FakeVaultAccessState(
        unlocked: Boolean,
    ) : VaultAccessState {
        var unlocked = unlocked
        private val stateFlow = MutableStateFlow<AuthenticationState>(
            if (unlocked) AuthenticationState.Authenticated(1L) else AuthenticationState.Locked
        )

        override val authenticationState: StateFlow<AuthenticationState> = stateFlow
        override fun isUnlocked(): Boolean = unlocked
    }

    private class FakeAuthenticationManager(
        private val vault: FakeVaultAccessState,
    ) : AuthenticationManager {
        override val state = MutableStateFlow<AuthenticationState>(AuthenticationState.Locked)
        override val methods = MutableStateFlow(AuthMethodAvailability(appPassword = true))
        override val databaseFailure = MutableStateFlow<Throwable?>(null)
        var lastLockReason: LockReason? = null

        override suspend fun authenticate(
            request: AuthenticationRequest,
            credential: CharArray?,
        ): AuthenticationResult {
            vault.unlocked = true
            return AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD)
        }

        override fun authenticate(
            request: AuthenticationRequest,
            callback: AuthenticationCallback,
        ): AuthenticationRequestHandle = error("Not used")

        override suspend fun lock(reason: LockReason) {
            lastLockReason = reason
            vault.unlocked = false
        }

        override suspend fun completeDatabaseRecovery(): Boolean = false
        override fun clearDatabaseFailure() = Unit
        override suspend fun refreshAvailability() = Unit
        override fun snapshot() = AuthenticationSnapshot(state.value, null, null)
        override fun onUserInteraction() = Unit
    }
}
