package com.aozijx.passly.feature.backup.internal.security

import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationMethods
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationSnapshot
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupAuthorizationPolicyTest {

    @Test
    fun `recovery and locked sessions are denied without authentication`() = runBlocking {
        listOf(AuthenticationState.RecoveryMode(1L), AuthenticationState.Locked).forEach { state ->
            val session = FakeSession(state)
            val authentication = FakeAuthenticationManager(session)

            assertEquals(
                BackupAuthorizationResult.Denied,
                BackupAuthorizationPolicy(authentication, session)
                    .authorize(AuthenticationPurpose.BACKUP_EXPORT),
            )
            assertEquals(0, authentication.requestCount)
        }
    }

    @Test
    fun `session expiration during fresh authentication fails closed`() = runBlocking {
        val session = FakeSession(AuthenticationState.Authenticated(1L))
        val authentication = FakeAuthenticationManager(session) {
            session.state.value = AuthenticationState.Locked
            AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD)
        }

        assertEquals(
            BackupAuthorizationResult.Denied,
            BackupAuthorizationPolicy(authentication, session)
                .authorize(AuthenticationPurpose.BACKUP_IMPORT),
        )
    }

    @Test
    fun `fresh authenticated session permits backup operation`() = runBlocking {
        val session = FakeSession(AuthenticationState.Authenticated(1L))
        val authentication = FakeAuthenticationManager(session)

        assertEquals(
            BackupAuthorizationResult.Authorized,
            BackupAuthorizationPolicy(authentication, session)
                .authorize(AuthenticationPurpose.BACKUP_EXPORT),
        )
    }

    private class FakeSession(initial: AuthenticationState) : SecureSessionAccessState {
        val state = MutableStateFlow(initial)
        override val authenticationState = state
        override fun isUnlocked(): Boolean = state.value is AuthenticationState.Authenticated ||
            state.value is AuthenticationState.RecoveryMode
    }

    private class FakeAuthenticationManager(
        private val session: FakeSession,
        private val result: () -> AuthenticationResult = {
            AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD)
        },
    ) : AuthenticationManager {
        override val state = session.state
        override val methods = MutableStateFlow(AuthenticationMethods(setOf(AuthenticationMethod.APP_PASSWORD)))
        var requestCount = 0

        override suspend fun authenticate(request: AuthenticationRequest, input: AuthInput): AuthenticationResult {
            requestCount++
            return result()
        }

        override suspend fun lock(reason: LockReason) {
            session.state.value = AuthenticationState.Locked
        }

        override suspend fun completeDatabaseRecovery(): Boolean = false
        override suspend fun refreshAvailability() = Unit
        override fun snapshot() = AuthenticationSnapshot(state.value, methods.value)
    }
}
