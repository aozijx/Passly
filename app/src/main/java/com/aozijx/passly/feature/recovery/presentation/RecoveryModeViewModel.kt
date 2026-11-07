package com.aozijx.passly.feature.recovery.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.crypto.MemoryCleaner
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.port.AuthenticationMethodProvisioner
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.feature.recovery.presentation.RecoveryModeEffect
import com.aozijx.passly.feature.recovery.presentation.RecoveryModeMutation
import com.aozijx.passly.feature.recovery.presentation.RecoveryModeReducer
import com.aozijx.passly.feature.recovery.presentation.RecoveryModeUiAction
import com.aozijx.passly.feature.recovery.presentation.RecoveryModeUiState
import com.aozijx.passly.presentation.shared.event.PageEffects
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@HiltViewModel
class RecoveryModeViewModel
    @Inject
    constructor(
        private val secureSessionAccessState: SecureSessionAccessState,
        private val methodProvisioner: AuthenticationMethodProvisioner,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(RecoveryModeUiState())
        val uiState: StateFlow<RecoveryModeUiState> = _uiState.asStateFlow()

        private val pageEffects = PageEffects<RecoveryModeEffect>()
        val effect = pageEffects.effects
        private var passwordProvisioning = false

        fun onAction(action: RecoveryModeUiAction) {
            when (action) {
                RecoveryModeUiAction.SetPasswordClicked -> showSetPasswordDialog()
                is RecoveryModeUiAction.NewPasswordChanged -> updateNewPassword(action.value)
                is RecoveryModeUiAction.ConfirmPasswordChanged -> updateConfirmPassword(action.value)
                RecoveryModeUiAction.SubmitNewPassword -> submitNewPassword()
                RecoveryModeUiAction.ExitClicked -> exitRecovery()
                RecoveryModeUiAction.DismissPasswordDialog -> dismissPasswordDialog()
            }
        }

        private fun showSetPasswordDialog() {
            if (passwordProvisioning) return
            if (!ensureRecoveryMode()) return
            wipePasswords()
            mutate(RecoveryModeMutation.PasswordDialogOpened)
        }

        private fun updateNewPassword(value: String) {
            val setup = _uiState.value.passwordSetup as? RecoveryPasswordSetup.Editing ?: return
            if (passwordProvisioning || setup.phase == RecoveryPasswordSetupPhase.Saving) return
            setup.newPassword.wipe()
            mutate(RecoveryModeMutation.NewPasswordChanged(OwnedChars.fromString(value)))
        }

        private fun updateConfirmPassword(value: String) {
            val setup = _uiState.value.passwordSetup as? RecoveryPasswordSetup.Editing ?: return
            if (passwordProvisioning || setup.phase == RecoveryPasswordSetupPhase.Saving) return
            setup.confirmPassword.wipe()
            mutate(RecoveryModeMutation.ConfirmPasswordChanged(OwnedChars.fromString(value)))
        }

        private fun submitNewPassword() {
            if (passwordProvisioning) return
            val state = _uiState.value
            if (!ensureRecoveryMode()) return
            val owner = pageEffects.currentOwner ?: return
            val setup = state.passwordSetup as? RecoveryPasswordSetup.Editing ?: return
            if (setup.phase == RecoveryPasswordSetupPhase.Saving) return
            val password = setup.newPassword.toCharArray()
            val confirm = setup.confirmPassword.toCharArray()

            if (password.isEmpty() || !password.contentEquals(confirm)) {
                MemoryCleaner.wipeCharArray(password)
                MemoryCleaner.wipeCharArray(confirm)
                mutate(
                    RecoveryModeMutation.ValidationFailed(
                        RecoveryPasswordSetupFailure.PasswordMismatchOrEmpty,
                    ),
                )
                return
            }

            passwordProvisioning = true
            mutate(RecoveryModeMutation.PasswordSetupStarted)
            viewModelScope.launch {
                try {
                    val result = methodProvisioner.setAppPassword(password)
                    if (!pageEffects.isActive(owner)) return@launch
                    when (result) {
                        is AuthenticationResult.Success -> {
                            wipePasswords()
                            mutate(RecoveryModeMutation.PasswordSetupCompleted)
                            emitEffect(owner, RecoveryModeEffect.PasswordResetCompleted)
                        }

                        is AuthenticationResult.Cancelled -> {
                            mutate(RecoveryModeMutation.PasswordSetupStopped)
                        }

                        is AuthenticationResult.Failure -> {
                            mutate(
                                RecoveryModeMutation.PasswordSetupFailed(
                                    RecoveryPasswordSetupFailure.SetupFailed,
                                ),
                            )
                        }
                    }
                } catch (cancelled: CancellationException) {
                    if (pageEffects.isActive(owner)) mutate(RecoveryModeMutation.PasswordSetupStopped)
                    throw cancelled
                } catch (_: Exception) {
                    if (pageEffects.isActive(owner)) {
                        mutate(
                            RecoveryModeMutation.PasswordSetupFailed(
                                RecoveryPasswordSetupFailure.SetupFailed,
                            ),
                        )
                    }
                } finally {
                    MemoryCleaner.wipeCharArray(password)
                    MemoryCleaner.wipeCharArray(confirm)
                    if (pageEffects.isActive(owner)) mutate(RecoveryModeMutation.PasswordSetupStopped)
                    passwordProvisioning = false
                }
            }
        }

        private fun exitRecovery() {
            val owner = pageEffects.currentOwner ?: return
            viewModelScope.launch {
                emitEffect(owner, RecoveryModeEffect.ExitRecovery)
            }
        }

        fun onPageEntered(): Any = pageEffects.enter()

        fun onPageExited(owner: Any) {
            if (!pageEffects.isActive(owner)) return
            pageEffects.exit(owner)
            wipePasswords()
            mutate(RecoveryModeMutation.PasswordDialogDiscarded)
        }

        private fun emitEffect(
            owner: Any,
            effect: RecoveryModeEffect,
        ) {
            pageEffects.tryEmit(owner, effect)
        }

        private fun dismissPasswordDialog() {
            if (passwordProvisioning) return
            wipePasswords()
            mutate(RecoveryModeMutation.PasswordDialogDismissed)
        }

        private fun ensureRecoveryMode(): Boolean {
            val recoveryMode = secureSessionAccessState.isRecoveryMode()
            if (recoveryMode) return true
            wipePasswords()
            mutate(RecoveryModeMutation.RecoveryModeRejected)
            return false
        }

        private fun mutate(mutation: RecoveryModeMutation) {
            _uiState.value = RecoveryModeReducer.reduce(_uiState.value, mutation)
        }

        private fun wipePasswords() {
            val setup = _uiState.value.passwordSetup as? RecoveryPasswordSetup.Editing ?: return
            setup.newPassword.wipe()
            setup.confirmPassword.wipe()
        }

        override fun onCleared() {
            wipePasswords()
        }
    }
