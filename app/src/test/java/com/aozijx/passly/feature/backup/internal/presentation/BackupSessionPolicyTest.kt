package com.aozijx.passly.feature.backup.internal.presentation

import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupSessionPolicyTest {
    @Test
    fun `recovery mode rejects export import and pending backup operations`() {
        val policy = BackupSessionPolicy(FixedSession(AuthenticationState.RecoveryMode(1L)))

        assertEquals(BackupSessionDenial.FULL_VAULT_ACCESS_REQUIRED, policy.regularExportDenial())
        assertEquals(BackupSessionDenial.FULL_VAULT_ACCESS_REQUIRED, policy.importDenial())
        assertEquals(BackupSessionDenial.FULL_VAULT_ACCESS_REQUIRED, policy.pendingOperationDenial())
    }

    @Test
    fun `authenticated mode permits backup operations`() {
        val policy = BackupSessionPolicy(FixedSession(AuthenticationState.Authenticated(1L)))

        assertNull(policy.regularExportDenial())
        assertNull(policy.importDenial())
        assertNull(policy.pendingOperationDenial())
    }

    private class FixedSession(state: AuthenticationState) : SecureSessionAccessState {
        override val authenticationState = MutableStateFlow(state)
        override fun isUnlocked(): Boolean = authenticationState.value !is AuthenticationState.Locked
    }
}
