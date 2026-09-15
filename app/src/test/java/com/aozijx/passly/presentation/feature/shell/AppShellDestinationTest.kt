package com.aozijx.passly.presentation.feature.shell

import org.junit.Assert.assertEquals
import org.junit.Test

class AppShellDestinationTest {

    @Test
    fun `database failure takes precedence over session destinations`() {
        val state = AppShellUiState(
            isAuthorized = true,
            isRecoveryMode = true,
            databaseError = IllegalStateException("broken"),
        )

        assertEquals(AppShellDestination.DATABASE_ERROR, state.destination)
    }

    @Test
    fun `session state resolves to one typed destination`() {
        assertEquals(
            AppShellDestination.VAULT,
            AppShellUiState(isAuthorized = true).destination,
        )
        assertEquals(
            AppShellDestination.RECOVERY,
            AppShellUiState(isRecoveryMode = true).destination,
        )
        assertEquals(
            AppShellDestination.AUTHENTICATION,
            AppShellUiState().destination,
        )
    }
}
