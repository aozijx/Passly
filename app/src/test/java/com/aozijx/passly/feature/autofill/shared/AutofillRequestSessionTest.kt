package com.aozijx.passly.feature.autofill.shared

import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationMethods
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationSnapshot
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.CancellationReason
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.port.AutofillGrantStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillRequestSessionTest {

    private fun session(
        authentication: AuthenticationManager,
        vault: SecureSessionAccessState,
        grantStore: AutofillGrantStore = FakeAutofillGrantStore(),
    ) = AutofillRequestSession(
        authenticationManager = authentication,
        vaultAccessState = vault,
        grantStore = grantStore,
        sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
    )

    @Test
    fun `request relocks only the full vault unlock it acquired`() = runBlocking {
        val vault = FakeSecureSessionAccessState(AuthenticationState.Locked)
        val authentication = FakeAuthenticationManager(vault)
        val requestSession = session(authentication, vault)

        assertEquals(
            AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD),
            requestSession.authenticate(),
        )
        requestSession.close()

        assertEquals(LockReason.AUTOFILL_REQUEST_FINISHED, authentication.lastLockReason)
    }

    @Test
    fun `request does not lock a vault that was already fully unlocked`() = runBlocking {
        val vault = FakeSecureSessionAccessState(AuthenticationState.Authenticated(1L))
        val authentication = FakeAuthenticationManager(vault)
        val requestSession = session(authentication, vault)

        requestSession.trackUnlock { "result" }
        requestSession.close()

        assertNull(authentication.lastLockReason)
    }

    @Test
    fun `request does not claim a restricted recovery session`() = runBlocking {
        val vault = FakeSecureSessionAccessState(AuthenticationState.RecoveryMode(1L))
        val authentication = FakeAuthenticationManager(vault, authenticationSucceeds = false)
        val requestSession = session(authentication, vault)

        requestSession.authenticate()
        requestSession.close()

        assertNull(authentication.lastLockReason)
    }

    @Test
    fun `terminal close revokes the request grant`() = runBlocking {
        val vault = FakeSecureSessionAccessState(AuthenticationState.Authenticated(1L))
        val authentication = FakeAuthenticationManager(vault)
        val grantStore = FakeAutofillGrantStore()
        val requestSession = session(authentication, vault, grantStore)
        val context = AutofillGrantContext("com.example.target", null)

        requestSession.grant(context)
        assertTrue(grantStore.isGranted(context))

        requestSession.close()

        assertFalse(grantStore.isGranted(context))
    }

    @Test
    fun `owner clear closes outside the cancelled view model scope`() = runBlocking {
        val vault = FakeSecureSessionAccessState(AuthenticationState.Locked)
        val authentication = FakeAuthenticationManager(vault)
        val grantStore = FakeAutofillGrantStore()
        val requestSession = session(authentication, vault, grantStore)
        val context = AutofillGrantContext("com.example.target", null)
        requestSession.authenticate()
        requestSession.grant(context)

        requestSession.closeOnOwnerCleared()

        assertEquals(LockReason.AUTOFILL_REQUEST_FINISHED, authentication.lastLockReason)
        assertFalse(grantStore.isGranted(context))
    }

    private class FakeSecureSessionAccessState(
        initialState: AuthenticationState,
    ) : SecureSessionAccessState {
        private val stateFlow = MutableStateFlow(initialState)
        override val authenticationState: StateFlow<AuthenticationState> = stateFlow

        override fun isUnlocked(): Boolean = stateFlow.value !is AuthenticationState.Locked

        fun authenticate() {
            stateFlow.value = AuthenticationState.Authenticated(1L)
        }

        fun lock() {
            stateFlow.value = AuthenticationState.Locked
        }
    }

    private class FakeAuthenticationManager(
        private val vault: FakeSecureSessionAccessState,
        private val authenticationSucceeds: Boolean = true,
    ) : AuthenticationManager {
        override val state = MutableStateFlow<AuthenticationState>(vault.authenticationState.value)
        override val methods = MutableStateFlow(
            AuthenticationMethods(setOf(AuthenticationMethod.APP_PASSWORD))
        )
        var lastLockReason: LockReason? = null

        override suspend fun authenticate(
            request: AuthenticationRequest,
            input: AuthInput,
        ): AuthenticationResult {
            if (!authenticationSucceeds) {
                return AuthenticationResult.Cancelled(CancellationReason.USER)
            }
            vault.authenticate()
            return AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD)
        }

        override suspend fun lock(reason: LockReason) {
            lastLockReason = reason
            vault.lock()
        }

        override suspend fun completeDatabaseRecovery(): Boolean = false
        override suspend fun refreshAvailability() = Unit
        override fun snapshot() = AuthenticationSnapshot(state.value, methods.value)
    }

    private class FakeAutofillGrantStore : AutofillGrantStore {
        private var active: AutofillGrantContext? = null

        override fun grant(context: AutofillGrantContext) {
            active = context
        }

        override fun isGranted(context: AutofillGrantContext): Boolean = active == context

        override fun clear() {
            active = null
        }
    }
}
