package com.aozijx.passly.presentation.feature.shell

import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationRequestId
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.LockReason
import org.junit.Assert.assertEquals
import org.junit.Test

class AppShellDestinationTest {

    @Test
    fun `database failure takes precedence over session destinations`() {
        val state = AppShellUiState(
            sessionMode = AppShellSessionMode.VAULT,
            databaseError = IllegalStateException("broken"),
        )

        assertEquals(AppShellDestination.DATABASE_ERROR, state.destination)
    }

    @Test
    fun `session state resolves to one typed destination`() {
        assertEquals(
            AppShellDestination.VAULT,
            AppShellUiState(sessionMode = AppShellSessionMode.VAULT).destination,
        )
        assertEquals(
            AppShellDestination.RECOVERY,
            AppShellUiState(sessionMode = AppShellSessionMode.RECOVERY).destination,
        )
        assertEquals(
            AppShellDestination.AUTHENTICATION,
            AppShellUiState().destination,
        )
    }

    @Test
    fun `authentication state maps exhaustively to one shell session mode`() {
        val requestId = AuthenticationRequestId("request")
        val authenticationStates = listOf(
            AuthenticationState.Locked,
            AuthenticationState.AwaitingHost(requestId),
            AuthenticationState.Authenticating(requestId, AuthenticationMethod.APP_PASSWORD),
            AuthenticationState.Unlocking(requestId),
            AuthenticationState.Locking(LockReason.USER),
        )

        authenticationStates.forEach { state ->
            assertEquals(AppShellSessionMode.AUTHENTICATION, state.toAppShellSessionMode())
        }
        assertEquals(
            AppShellSessionMode.VAULT,
            AuthenticationState.Authenticated(authenticatedAtMs = 1L).toAppShellSessionMode(),
        )
        assertEquals(
            AppShellSessionMode.RECOVERY,
            AuthenticationState.RecoveryMode(authenticatedAtMs = 1L).toAppShellSessionMode(),
        )
    }
}
