package com.aozijx.passly.feature.database.presentation.reset

import com.aozijx.passly.R
import com.aozijx.passly.core.error.model.Unexpected
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
import com.aozijx.passly.presentation.shared.error.UiErrorMessage
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DatabaseResetViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun failedResetKeepsAResourceKeyRatherThanResolvedCopy() =
        runTest {
            val gateway =
                object : DatabaseResetGateway {
                    override suspend fun reset(): AppResult<Unit> = AppResult.Failure(Unexpected())
                }
            val model = viewModel(null, gateway)
            model.onAction(DatabaseResetUiAction.Reset)
            val failedState = model.uiState.first { it is DatabaseResetUiState.Failed } as DatabaseResetUiState.Failed
            val failure = failedState.reason as DatabaseResetFailure.Unexpected
            assertEquals(UiErrorMessage(R.string.database_reset_error_failed), failure.message)
        }

    @Test
    fun cancelledAuthorizationDoesNotResetOrLeaveBusyState() =
        runTest {
            val gateway = RecordingDatabaseGateway()
            val viewModel = viewModel(AuthorizationResult.Cancelled, gateway)

            viewModel.onAction(DatabaseResetUiAction.Reset)
            val settled = viewModel.uiState.first { it != DatabaseResetUiState.Resetting }

            assertSame(DatabaseResetUiState.Idle, settled)
            assertEquals(0, gateway.resetCount)
        }

    @Test
    fun successfulAuthorizationResetsDatabaseAndCompletes() =
        runTest {
            val gateway = RecordingDatabaseGateway()
            val viewModel = viewModel(null, gateway)

            viewModel.onAction(DatabaseResetUiAction.Reset)
            val completed = viewModel.uiState.first { it == DatabaseResetUiState.Completed }

            assertSame(DatabaseResetUiState.Completed, completed)
            assertEquals(1, gateway.resetCount)
        }

    @Test
    fun completedResetReturnsToIdleWhenTheSheetPageIsReentered() =
        runTest {
            val gateway = RecordingDatabaseGateway()
            val viewModel = viewModel(null, gateway)
            val firstOwner = viewModel.onPageEntered()

            viewModel.onAction(DatabaseResetUiAction.Reset)
            viewModel.uiState.first { it == DatabaseResetUiState.Completed }
            viewModel.onPageExited(firstOwner)

            assertSame(DatabaseResetUiState.Idle, viewModel.uiState.value)
            val secondOwner = viewModel.onPageEntered()
            viewModel.onAction(DatabaseResetUiAction.Reset)
            viewModel.uiState.first { it == DatabaseResetUiState.Completed }
            viewModel.onPageExited(secondOwner)

            assertEquals(2, gateway.resetCount)
        }

    @Test
    fun olderSuccessTimerCannotClearNewerResetFeedback() =
        runTest {
            val gateway = RecordingDatabaseGateway()
            val viewModel = viewModel(null, gateway)
            val firstOwner = viewModel.onPageEntered()

            viewModel.onAction(DatabaseResetUiAction.Reset)
            runCurrent()
            assertSame(DatabaseResetUiState.Completed, viewModel.uiState.value)
            viewModel.onPageExited(firstOwner)

            advanceTimeBy(1_500)
            runCurrent()
            viewModel.onPageEntered()
            viewModel.onAction(DatabaseResetUiAction.Reset)
            runCurrent()
            assertSame(DatabaseResetUiState.Completed, viewModel.uiState.value)

            advanceTimeBy(1_500)
            runCurrent()

            assertSame(DatabaseResetUiState.Completed, viewModel.uiState.value)
            advanceTimeBy(1_500)
            runCurrent()
            assertSame(DatabaseResetUiState.Idle, viewModel.uiState.value)
            assertEquals(2, gateway.resetCount)
        }

    @Test
    fun leavingWhileResettingDoesNotCancelTheResetAndReentryStartsFresh() =
        runTest {
            val resetResult = CompletableDeferred<AppResult<Unit>>()
            var resetCount = 0
            val gateway =
                object : DatabaseResetGateway {
                    override suspend fun reset(): AppResult<Unit> {
                        resetCount++
                        return resetResult.await()
                    }
                }
            val viewModel = viewModel(null, gateway)
            val firstOwner = viewModel.onPageEntered()

            viewModel.onAction(DatabaseResetUiAction.Reset)
            runCurrent()
            assertSame(DatabaseResetUiState.Resetting, viewModel.uiState.value)
            viewModel.onPageExited(firstOwner)
            assertSame(DatabaseResetUiState.Resetting, viewModel.uiState.value)

            resetResult.complete(AppResult.Success(Unit))
            runCurrent()
            assertSame(DatabaseResetUiState.Completed, viewModel.uiState.value)

            val secondOwner = viewModel.onPageEntered()
            assertSame(DatabaseResetUiState.Idle, viewModel.uiState.value)
            viewModel.onPageExited(secondOwner)
            assertEquals(1, resetCount)
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
