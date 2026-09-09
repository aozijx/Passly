package com.aozijx.passly.presentation.feature.database.reset

import com.aozijx.passly.core.error.result.AppResult
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
import com.aozijx.passly.feature.database.reset.DatabaseResetGateway
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DatabaseResetViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun cancelledAuthenticationDoesNotResetOrLeaveBusyState() {
        val controller = RecordingDatabaseController()
        val viewModel = DatabaseResetViewModel(
            secureSessionAccessState = UnlockedSessionAccessState,
            authenticationManager = FixedAuthenticationManager(
                AuthenticationResult.Cancelled(CancellationReason.USER),
            ),
            databaseResetGateway = controller,
        )

        viewModel.onAction(DatabaseResetUiAction.Reset)

        assertFalse(viewModel.uiState.value.isResetting)
        assertFalse(viewModel.uiState.value.isResetComplete)
        assertTrue(controller.resetCount == 0)
    }

    @Test
    fun successfulAuthenticationResetsDatabaseAndCompletes() = runTest {
        val controller = RecordingDatabaseController()
        val viewModel = DatabaseResetViewModel(
            secureSessionAccessState = UnlockedSessionAccessState,
            authenticationManager = FixedAuthenticationManager(
                AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD),
            ),
            databaseResetGateway = controller,
        )

        viewModel.onAction(DatabaseResetUiAction.Reset)

        val completed = viewModel.uiState.first { it.isResetComplete }

        assertFalse(completed.isResetting)
        assertTrue(controller.resetCount == 1)
    }

    private data object UnlockedSessionAccessState : SecureSessionAccessState {
        override val authenticationState: StateFlow<AuthenticationState> =
            MutableStateFlow(AuthenticationState.Authenticated(1L))

        override fun isUnlocked() = true
    }

    private class FixedAuthenticationManager(
        private val result: AuthenticationResult,
    ) : AuthenticationManager {
        override val state: StateFlow<AuthenticationState> =
            MutableStateFlow(AuthenticationState.Authenticated(1L))
        override val methods: StateFlow<AuthenticationMethods> =
            MutableStateFlow(AuthenticationMethods(setOf(AuthenticationMethod.APP_PASSWORD)))

        override suspend fun authenticate(
            request: AuthenticationRequest,
            input: AuthInput,
        ) = result

        override suspend fun lock(reason: LockReason) = Unit
        override suspend fun refreshAvailability() = Unit
        override fun snapshot() = AuthenticationSnapshot(state.value, methods.value)
    }

    private class RecordingDatabaseController : DatabaseResetGateway {
        var resetCount = 0

        override suspend fun reset(): AppResult<Unit> {
            resetCount++
            return AppResult.Success(Unit)
        }
    }
}
