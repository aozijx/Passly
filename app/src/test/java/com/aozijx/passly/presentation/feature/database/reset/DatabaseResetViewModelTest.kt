package com.aozijx.passly.presentation.feature.database.reset

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.feature.database.reset.DatabaseResetGateway
import com.aozijx.passly.feature.database.reset.ResetDatabaseUseCase
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DatabaseResetViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun cancelledAuthorizationDoesNotResetOrLeaveBusyState() = runTest {
        val gateway = RecordingDatabaseGateway()
        val viewModel = viewModel(AuthorizationResult.Cancelled, gateway)

        viewModel.onAction(DatabaseResetUiAction.Reset)
        val settled = viewModel.uiState.first { !it.isResetting }

        assertFalse(settled.isResetComplete)
        assertEquals(0, gateway.resetCount)
    }

    @Test
    fun successfulAuthorizationResetsDatabaseAndCompletes() = runTest {
        val gateway = RecordingDatabaseGateway()
        val viewModel = viewModel(null, gateway)

        viewModel.onAction(DatabaseResetUiAction.Reset)
        val completed = viewModel.uiState.first { it.isResetComplete }

        assertFalse(completed.isResetting)
        assertEquals(1, gateway.resetCount)
    }

    private fun viewModel(
        terminalResult: AuthorizationResult<Nothing>?,
        gateway: DatabaseResetGateway,
    ) = DatabaseResetViewModel(
        ResetDatabaseUseCase(
            secureSessionAccessState = FullAccess,
            authorizationGate = FixedAuthorizationGate(terminalResult),
            databaseResetGateway = gateway,
        ),
    )

    private class FixedAuthorizationGate(
        private val terminalResult: AuthorizationResult<Nothing>?,
    ) : AuthorizationGate {
        override suspend fun <T> authorize(
            scope: AuthorizationScope,
            input: AuthInput,
            block: suspend (AuthorizationPermit) -> T,
        ): AuthorizationResult<T> {
            terminalResult?.let {
                @Suppress("UNCHECKED_CAST")
                return it as AuthorizationResult<T>
            }
            return AuthorizationResult.Allowed(block(object : AuthorizationPermit {}))
        }
    }

    private object FullAccess : SecureSessionAccessState {
        override val authenticationState: StateFlow<AuthenticationState> =
            MutableStateFlow(AuthenticationState.Authenticated(1L))
        override fun isUnlocked() = true
    }

    private class RecordingDatabaseGateway : DatabaseResetGateway {
        var resetCount = 0
        override suspend fun reset(): AppResult<Unit> {
            resetCount++
            return AppResult.Success(Unit)
        }
    }
}