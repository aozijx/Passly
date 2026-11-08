package com.aozijx.passly.feature.recovery.presentation

import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.CancellationReason
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.testing.MainDispatcherRule
import com.aozijx.passly.testing.RecordingAuthenticationMethodProvisioner
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecoveryModeViewModelTest {
    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `saving recovery password rejects edits reopen dismiss and duplicate submit`() =
        runTest {
            val gate = CompletableDeferred<AuthenticationResult>()
            val provisioner = RecordingAuthenticationMethodProvisioner { gate.await() }
            val viewModel = RecoveryModeViewModel(RecoverySession(), provisioner)
            viewModel.onPageEntered()

            viewModel.onAction(RecoveryModeUiAction.SetPasswordClicked)
            viewModel.onAction(RecoveryModeUiAction.NewPasswordChanged(PASSWORD))
            viewModel.onAction(RecoveryModeUiAction.ConfirmPasswordChanged(PASSWORD))
            val submittedPassword =
                (viewModel.uiState.value as RecoveryModeUiState.Editing).draft.newPassword as OwnedChars
            viewModel.onAction(RecoveryModeUiAction.SubmitNewPassword)
            runCurrent()

            val submittedSetup = viewModel.uiState.value as RecoveryModeUiState.Saving
            assertTrue(submittedSetup.draft.newPassword.isEmpty)

            viewModel.onAction(RecoveryModeUiAction.NewPasswordChanged("another-long-password"))
            viewModel.onAction(RecoveryModeUiAction.ConfirmPasswordChanged("another-long-password"))
            viewModel.onAction(RecoveryModeUiAction.SetPasswordClicked)
            viewModel.onAction(RecoveryModeUiAction.DismissPasswordDialog)
            viewModel.onAction(RecoveryModeUiAction.SubmitNewPassword)
            runCurrent()

            assertEquals(1, provisioner.calls)
            assertEquals(
                RecoveryModeUiState.Saving::class,
                viewModel.uiState.value::class,
            )

            gate.complete(AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD))
            runCurrent()

            assertEquals(RecoveryModeUiState.Closed, viewModel.uiState.value)
            assertTrue(submittedPassword.isWiped)
            assertTrue(provisioner.submitted.single().all { it == '\u0000' })
        }

    @Test
    fun `exited recovery page cannot reopen or receive an in flight password result`() =
        runTest {
            val gate = CompletableDeferred<AuthenticationResult>()
            val provisioner = RecordingAuthenticationMethodProvisioner { gate.await() }
            val viewModel = RecoveryModeViewModel(RecoverySession(), provisioner)
            val oldOwner = viewModel.onPageEntered()
            val effects = mutableListOf<RecoveryModeEffect>()
            backgroundScope.launch { viewModel.effect.toList(effects) }
            runCurrent()

            viewModel.onAction(RecoveryModeUiAction.SetPasswordClicked)
            viewModel.onAction(RecoveryModeUiAction.NewPasswordChanged(PASSWORD))
            viewModel.onAction(RecoveryModeUiAction.ConfirmPasswordChanged(PASSWORD))
            val submittedPassword =
                (viewModel.uiState.value as RecoveryModeUiState.Editing).draft.newPassword as OwnedChars
            viewModel.onAction(RecoveryModeUiAction.SubmitNewPassword)
            runCurrent()

            viewModel.onPageExited(oldOwner)
            viewModel.onPageEntered()
            viewModel.onAction(RecoveryModeUiAction.SetPasswordClicked)
            assertEquals(RecoveryModeUiState.Closed, viewModel.uiState.value)

            gate.complete(AuthenticationResult.Success(AuthenticationMethod.APP_PASSWORD))
            runCurrent()

            assertEquals(1, provisioner.calls)
            assertTrue(effects.isEmpty())
            assertTrue(submittedPassword.isWiped)
            assertEquals(RecoveryModeUiState.Closed, viewModel.uiState.value)

            viewModel.onAction(RecoveryModeUiAction.SetPasswordClicked)
            viewModel.onAction(RecoveryModeUiAction.NewPasswordChanged(PASSWORD))
            viewModel.onAction(RecoveryModeUiAction.ConfirmPasswordChanged(PASSWORD))
            viewModel.onAction(RecoveryModeUiAction.SubmitNewPassword)
            runCurrent()

            assertEquals(2, provisioner.calls)
            assertEquals(listOf(RecoveryModeEffect.PasswordResetCompleted), effects)
        }

    @Test
    fun `ordinary provisioning exception becomes a recovery setup failure and clears input`() =
        runTest {
            val provisioner = RecordingAuthenticationMethodProvisioner { throw IOException("store unavailable") }
            val viewModel = RecoveryModeViewModel(RecoverySession(), provisioner)
            viewModel.onPageEntered()
            viewModel.onAction(RecoveryModeUiAction.SetPasswordClicked)
            viewModel.onAction(RecoveryModeUiAction.NewPasswordChanged(PASSWORD))
            viewModel.onAction(RecoveryModeUiAction.ConfirmPasswordChanged(PASSWORD))
            viewModel.onAction(RecoveryModeUiAction.SubmitNewPassword)
            runCurrent()

            val setup = viewModel.uiState.value as RecoveryModeUiState.Failed
            assertEquals(RecoveryPasswordSetupFailure.SetupFailed, setup.reason)
            assertTrue(provisioner.submitted.single().all { it == '\u0000' })
        }

    @Test
    fun `cancelled recovery setup can retry and structured failure remains visible`() =
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
            val viewModel = RecoveryModeViewModel(RecoverySession(), provisioner)
            viewModel.onPageEntered()
            viewModel.onAction(RecoveryModeUiAction.SetPasswordClicked)
            viewModel.onAction(RecoveryModeUiAction.NewPasswordChanged(PASSWORD))
            viewModel.onAction(RecoveryModeUiAction.ConfirmPasswordChanged(PASSWORD))

            viewModel.onAction(RecoveryModeUiAction.SubmitNewPassword)
            runCurrent()
            assertEquals(
                RecoveryModeUiState.Editing::class,
                viewModel.uiState.value::class,
            )

            viewModel.onAction(RecoveryModeUiAction.NewPasswordChanged(PASSWORD))
            viewModel.onAction(RecoveryModeUiAction.ConfirmPasswordChanged(PASSWORD))
            viewModel.onAction(RecoveryModeUiAction.SubmitNewPassword)
            runCurrent()

            assertEquals(2, provisioner.calls)
            assertEquals(
                RecoveryModeUiState.Failed::class,
                viewModel.uiState.value::class,
            )
        }

    private class RecoverySession : SecureSessionAccessState {
        override val authenticationState =
            MutableStateFlow<AuthenticationState>(AuthenticationState.RecoveryMode(authenticatedAtMs = 1))

        override fun isUnlocked() = true
    }

    private companion object {
        const val PASSWORD = "long-enough-password"
    }
}
