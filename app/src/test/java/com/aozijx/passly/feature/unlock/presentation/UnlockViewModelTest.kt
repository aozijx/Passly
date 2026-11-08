package com.aozijx.passly.feature.unlock.presentation

import com.aozijx.passly.domain.access.model.AuthenticationKdfParameters
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationAttemptStatus
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationMethods
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationSnapshot
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.AttemptStatus
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.CancellationReason
import com.aozijx.passly.domain.access.model.SessionLockRequest
import com.aozijx.passly.domain.access.port.AuthenticationKdfParameterSource
import com.aozijx.passly.domain.access.port.AuthenticationAttemptStatusProvider
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.testing.MainDispatcherRule
import com.aozijx.passly.testing.RecordingAuthenticationMethodProvisioner
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UnlockViewModelTest {
    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `saving password ignores edits reopen dismiss and duplicate submit until result`() =
        runTest {
            val gate = CompletableDeferred<AuthenticationResult>()
            val provisioner = RecordingAuthenticationMethodProvisioner { gate.await() }
            val viewModel =
                UnlockViewModel(
                    authenticationManager = FakeAuthenticationManager(),
                    attemptStatusProvider = FakeAttemptStatusProvider(),
                    kdfParameterSource =
                        object : AuthenticationKdfParameterSource {
                            override suspend fun selected() = PARAMETERS
                        },
                    methodProvisioner = provisioner,
                )

            viewModel.onAction(UnlockUiAction.SetPasswordClicked)
            viewModel.onAction(UnlockUiAction.NewAppPasswordChanged(PASSWORD))
            viewModel.onAction(UnlockUiAction.ConfirmAppPasswordChanged(PASSWORD))
            val submittedPassword =
                (viewModel.uiState.value.passwordSetup as UnlockPasswordSetup.Editing).newPassword as OwnedChars
            viewModel.onAction(UnlockUiAction.SetPasswordConfirmed)
            runCurrent()

            val submittedSetup = viewModel.uiState.value.passwordSetup as UnlockPasswordSetup.Editing
            assertTrue(submittedSetup.newPassword.isEmpty)

            viewModel.onAction(UnlockUiAction.NewAppPasswordChanged("another-long-password"))
            viewModel.onAction(UnlockUiAction.ConfirmAppPasswordChanged("another-long-password"))
            viewModel.onAction(UnlockUiAction.SetPasswordClicked)
            viewModel.onAction(UnlockUiAction.DismissSetPasswordDialog)
            viewModel.onAction(UnlockUiAction.SetPasswordConfirmed)
            runCurrent()

            assertEquals(1, provisioner.calls)
            assertEquals(
                UnlockPasswordSetupPhase.Saving,
                (viewModel.uiState.value.passwordSetup as? UnlockPasswordSetup.Editing)?.phase,
            )

            gate.complete(AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD))
            runCurrent()

            assertEquals(UnlockPasswordSetup.Closed, viewModel.uiState.value.passwordSetup)
            assertTrue(submittedPassword.isWiped)
            assertTrue(provisioner.submitted.single().all { it == '\u0000' })
        }

    @Test
    fun `ordinary provisioning exception becomes a page failure and clears submitted input`() =
        runTest {
            val provisioner = RecordingAuthenticationMethodProvisioner { throw IOException("store unavailable") }
            val viewModel =
                UnlockViewModel(
                    authenticationManager = FakeAuthenticationManager(),
                    attemptStatusProvider = FakeAttemptStatusProvider(),
                    kdfParameterSource =
                        object : AuthenticationKdfParameterSource {
                            override suspend fun selected() = PARAMETERS
                        },
                    methodProvisioner = provisioner,
                )
            viewModel.onAction(UnlockUiAction.SetPasswordClicked)
            viewModel.onAction(UnlockUiAction.NewAppPasswordChanged(PASSWORD))
            viewModel.onAction(UnlockUiAction.ConfirmAppPasswordChanged(PASSWORD))
            viewModel.onAction(UnlockUiAction.SetPasswordConfirmed)
            runCurrent()

            val setup = viewModel.uiState.value.passwordSetup as UnlockPasswordSetup.Editing
            assertEquals(
                UnlockPasswordSetupPhase.Failed(UnlockFailureMessage.AppPasswordSetupFailed),
                setup.phase,
            )
            assertTrue(provisioner.submitted.single().all { it == '\u0000' })
        }

    @Test
    fun `cancelled setup can retry and a structured failure remains visible`() =
        runTest {
            val results =
                ArrayDeque(
                    listOf(
                        AuthenticationResult.Cancelled(CancellationReason.USER),
                        AuthenticationResult.Failure(
                            AuthenticationFailure(AuthenticationFailureCode.SESSION_TRANSITION_FAILED),
                        ),
                    ),
                )
            val provisioner = RecordingAuthenticationMethodProvisioner { results.removeFirst() }
            val viewModel =
                UnlockViewModel(
                    authenticationManager = FakeAuthenticationManager(),
                    attemptStatusProvider = FakeAttemptStatusProvider(),
                    kdfParameterSource =
                        object : AuthenticationKdfParameterSource {
                            override suspend fun selected() = PARAMETERS
                        },
                    methodProvisioner = provisioner,
                )
            viewModel.onAction(UnlockUiAction.SetPasswordClicked)
            viewModel.onAction(UnlockUiAction.NewAppPasswordChanged(PASSWORD))
            viewModel.onAction(UnlockUiAction.ConfirmAppPasswordChanged(PASSWORD))

            viewModel.onAction(UnlockUiAction.SetPasswordConfirmed)
            runCurrent()
            assertEquals(
                UnlockPasswordSetupPhase.Idle,
                (viewModel.uiState.value.passwordSetup as UnlockPasswordSetup.Editing).phase,
            )

            viewModel.onAction(UnlockUiAction.NewAppPasswordChanged(PASSWORD))
            viewModel.onAction(UnlockUiAction.ConfirmAppPasswordChanged(PASSWORD))
            viewModel.onAction(UnlockUiAction.SetPasswordConfirmed)
            runCurrent()

            assertEquals(2, provisioner.calls)
            assertEquals(
                UnlockPasswordSetupPhase.Failed(UnlockFailureMessage.AppPasswordSetupFailed),
                (viewModel.uiState.value.passwordSetup as UnlockPasswordSetup.Editing).phase,
            )
        }

    @Test
    fun `entry restores attempts and updates the lockout countdown`() =
        runTest {
            val statuses =
                ArrayDeque(
                    listOf(
                        attemptStatus(2_300L),
                        attemptStatus(1_300L),
                        attemptStatus(300L),
                        attemptStatus(null, used = 0),
                    ),
                )
            val provider = FakeAttemptStatusProvider { statuses.removeFirst() }
            val viewModel =
                UnlockViewModel(
                    authenticationManager =
                        FakeAuthenticationManager(
                            AuthenticationMethods(setOf(AuthenticationMethod.APP_PASSWORD)),
                        ),
                    attemptStatusProvider = provider,
                    kdfParameterSource =
                        object : AuthenticationKdfParameterSource {
                            override suspend fun selected() = PARAMETERS
                        },
                    methodProvisioner = RecordingAuthenticationMethodProvisioner { AuthenticationResult.Cancelled(CancellationReason.USER) },
                )

            runCurrent()

            val restored = viewModel.uiState.value.authenticationAttemptStatuses.getValue(AuthenticationMethod.APP_PASSWORD)
            assertEquals(5, restored.attempts.used)
            assertEquals(2_300L, restored.retryAfterMs)

            advanceTimeBy(1_000L)
            runCurrent()
            assertEquals(
                1_300L,
                viewModel.uiState.value.authenticationAttemptStatuses.getValue(AuthenticationMethod.APP_PASSWORD).retryAfterMs,
            )

            advanceTimeBy(2_000L)
            runCurrent()
            assertEquals(
                0,
                viewModel.uiState.value.authenticationAttemptStatuses.getValue(AuthenticationMethod.APP_PASSWORD).attempts.used,
            )
        }

    private class FakeAuthenticationManager(initialMethods: AuthenticationMethods = AuthenticationMethods()) : AuthenticationManager {
        override val methods = MutableStateFlow(initialMethods)
        override val state = MutableStateFlow<AuthenticationState>(AuthenticationState.Locked)

        override suspend fun authenticate(
            request: AuthenticationRequest,
            input: AuthInput,
        ) = AuthenticationResult.Cancelled(com.aozijx.passly.domain.access.model.CancellationReason.CALLER)

        override suspend fun lock(request: SessionLockRequest) = Unit

        override suspend fun refreshAvailability() = Unit

        override fun snapshot() = AuthenticationSnapshot(state.value, methods.value)
    }

    private class FakeAttemptStatusProvider(
        private val source: suspend (AuthenticationMethod) -> AuthenticationAttemptStatus = { attemptStatus(null, used = 0) },
    ) : AuthenticationAttemptStatusProvider {
        override suspend fun status(method: AuthenticationMethod): AuthenticationAttemptStatus = source(method)
    }

    private companion object {
        fun attemptStatus(
            retryAfterMs: Long?,
            used: Int = 5,
        ) = AuthenticationAttemptStatus(AttemptStatus(used = used, limit = 5), retryAfterMs)

        const val PASSWORD = "long-enough-password"
        val PARAMETERS = AuthenticationKdfParameters(0x13, 65_536, 3, 4, 256)
    }
}
