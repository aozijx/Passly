package com.aozijx.passly.feature.unlock.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.crypto.MemoryCleaner
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.port.AuthenticationAttemptStatusProvider
import com.aozijx.passly.domain.access.port.AuthenticationKdfParameterSource
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.port.AuthenticationMethodProvisioner
import com.aozijx.passly.domain.entry.policy.PasswordStrengthEvaluator
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.feature.unlock.presentation.UnlockUiAction
import com.aozijx.passly.feature.unlock.presentation.UnlockUiState
import com.aozijx.passly.presentation.mapping.PasswordSetupDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class UnlockViewModel
    @Inject
    constructor(
        private val authenticationManager: AuthenticationManager,
        private val attemptStatusProvider: AuthenticationAttemptStatusProvider,
        private val kdfParameterSource: AuthenticationKdfParameterSource,
        private val methodProvisioner: AuthenticationMethodProvisioner,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(
                UnlockUiState(availableMethods = authenticationManager.methods.value),
            )
        val uiState: StateFlow<UnlockUiState> = _uiState.asStateFlow()

        private var recoveryRevealTapCount = 0
        private var passwordProvisioning = false
        private val attemptStatusJobs = mutableMapOf<AuthenticationMethod, Job>()

        init {
            viewModelScope.launch {
                try {
                    mutate(UnlockMutation.KdfParametersReady(kdfParameterSource.selected()))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    mutate(UnlockMutation.KdfInitializationFailed)
                }
            }
            viewModelScope.launch {
                authenticationManager.methods.collect { methods ->
                    mutate(UnlockMutation.AvailableMethodsChanged(methods))
                }
            }
        }

        fun onAction(action: UnlockUiAction) {
            when (action) {
                UnlockUiAction.BiometricClicked -> {
                    verifyWithBiometric()
                }

                UnlockUiAction.LockIconClicked -> {
                    revealRecoveryUnlock()
                }

                UnlockUiAction.BackPressed -> {
                    onBackPressed()
                }

                is UnlockUiAction.AppPasswordChanged -> {
                    onAppPasswordChange(action.value)
                }

                UnlockUiAction.AppPasswordSubmitted -> {
                    verifyWithAppPassword()
                }

                is UnlockUiAction.RecoveryCodeChanged -> {
                    onRecoveryCodeChange(action.value)
                }

                UnlockUiAction.RecoveryCodeSubmitted -> {
                    unlockWithRecoveryCode()
                }

                is UnlockUiAction.InputExpanded -> {
                    onInputExpanded(action.method, action.expanded)
                }

                UnlockUiAction.ClearVerificationFailure -> {
                    clearVerificationFailure()
                }

                UnlockUiAction.SetPasswordClicked -> {
                    openSetPasswordDialog()
                }

                is UnlockUiAction.NewAppPasswordChanged -> {
                    updateNewAppPassword(action.value)
                }

                is UnlockUiAction.ConfirmAppPasswordChanged -> {
                    updateConfirmAppPassword(action.value)
                }

                UnlockUiAction.SetPasswordConfirmed -> {
                    setAppPassword()
                }

                UnlockUiAction.DismissSetPasswordDialog -> {
                    dismissSetPasswordDialog()
                }
            }
        }

        private fun updateNewAppPassword(value: String) {
            val draft = editablePasswordDraft() ?: return
            if (passwordProvisioning) return
            draft.newPassword.wipe()
            mutate(
                UnlockMutation.NewAppPasswordChanged(
                    value = OwnedChars.fromString(value),
                    passwordStrength =
                        value
                            .takeIf(String::isNotEmpty)
                            ?.let(PasswordStrengthEvaluator::evaluate),
                ),
            )
        }

        private fun updateConfirmAppPassword(value: String) {
            val draft = editablePasswordDraft() ?: return
            if (passwordProvisioning) return
            draft.confirmPassword.wipe()
            mutate(UnlockMutation.ConfirmAppPasswordChanged(OwnedChars.fromString(value)))
        }

        private fun dismissSetPasswordDialog() {
            if (passwordProvisioning) return
            wipeSetupPasswords()
            mutate(UnlockMutation.PasswordSetupClosed)
        }

        private fun openSetPasswordDialog() {
            if (passwordProvisioning) return
            wipeSetupPasswords()
            mutate(UnlockMutation.PasswordSetupOpened)
        }

        private fun setAppPassword() {
            if (passwordProvisioning) return
            val draft = editablePasswordDraft() ?: return
            val password = draft.newPassword.toCharArray()
            val confirm = draft.confirmPassword.toCharArray()
            if (!draft.canSubmit()) {
                MemoryCleaner.wipeCharArray(password)
                MemoryCleaner.wipeCharArray(confirm)
                return
            }
            passwordProvisioning = true
            wipeSetupPasswords()
            mutate(UnlockMutation.PasswordSetupStarted)
            viewModelScope.launch {
                try {
                    when (val result = methodProvisioner.setAppPassword(password)) {
                        is AuthenticationResult.Success -> {
                            wipeSetupPasswords()
                            mutate(UnlockMutation.PasswordSetupCompleted)
                        }

                        is AuthenticationResult.Cancelled -> {
                            Unit
                        }

                        is AuthenticationResult.Failure -> {
                            mutate(UnlockMutation.PasswordSetupFailed(result.failure))
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    mutate(
                        UnlockMutation.PasswordSetupFailed(
                            AuthenticationFailure(AuthenticationFailureCode.SESSION_TRANSITION_FAILED),
                        ),
                    )
                } finally {
                    MemoryCleaner.wipeCharArray(password)
                    MemoryCleaner.wipeCharArray(confirm)
                    mutate(UnlockMutation.PasswordSetupFinished)
                    passwordProvisioning = false
                }
            }
        }

        private fun revealRecoveryUnlock() {
            val state = _uiState.value
            if (AuthenticationMethod.RECOVERY_CODE !in state.availableMethods || state.recoveryUnlockVisible) return
            recoveryRevealTapCount += 1
            if (recoveryRevealTapCount >= RECOVERY_REVEAL_TAP_THRESHOLD) {
                recoveryRevealTapCount = 0
                mutate(UnlockMutation.RecoveryUnlockVisibilityChanged(true))
            }
        }

        private fun onBackPressed() {
            _uiState.value.inputExpansion?.let { onInputExpanded(it, false) }
                ?: run {
                    recoveryRevealTapCount = 0
                    mutate(UnlockMutation.RecoveryUnlockVisibilityChanged(false))
                }
        }

        private fun verifyWithBiometric() {
            resetInputState()
            authenticate(AuthenticationMethod.BIOMETRIC)
        }

        private fun onAppPasswordChange(value: String) {
            _uiState.value.appPassword.wipe()
            mutate(UnlockMutation.AppPasswordChanged(OwnedChars.fromString(value)))
        }

        private fun onRecoveryCodeChange(value: String) {
            _uiState.value.recoveryCode.wipe()
            mutate(UnlockMutation.RecoveryCodeChanged(OwnedChars.fromString(value)))
        }

        private fun onInputExpanded(
            method: AuthenticationMethod,
            expanded: Boolean,
        ) {
            if (expanded) {
                _uiState.value.inputExpansion
                    ?.takeUnless { it == method }
                    ?.let(::clearCredential)
                mutate(UnlockMutation.InputExpansionChanged(method))
            } else {
                clearCredential(method)
                if (_uiState.value.inputExpansion == method) {
                    mutate(UnlockMutation.InputExpansionChanged(null))
                }
            }
        }

        private fun verifyWithAppPassword() {
            authenticate(
                method = AuthenticationMethod.APP_PASSWORD,
                credential = _uiState.value.appPassword.toCharArray(),
            )
        }

        private fun unlockWithRecoveryCode() {
            authenticate(
                method = AuthenticationMethod.RECOVERY_CODE,
                credential = _uiState.value.recoveryCode.toCharArray(),
                purpose = AuthenticationPurpose.RECOVER_AUTH_METHODS,
            )
        }

        private fun clearVerificationFailure() {
            mutate(UnlockMutation.VerificationFailureCleared)
        }

        private fun authenticate(
            method: AuthenticationMethod,
            credential: CharArray? = null,
            purpose: AuthenticationPurpose = AuthenticationPurpose.UNLOCK_VAULT,
        ) {
            if (_uiState.value.verificationPhase is UnlockVerificationPhase.Authenticating) {
                credential?.let(MemoryCleaner::wipeCharArray)
                return
            }
            mutate(UnlockMutation.AuthenticationStarted(method))
            viewModelScope.launch {
                try {
                    when (
                        val result =
                            authenticationManager.authenticate(
                                AuthenticationRequest(
                                    purpose = purpose,
                                    allowedMethods = setOf(method),
                                ),
                                when (method) {
                                    AuthenticationMethod.BIOMETRIC -> {
                                        AuthInput.Interactive
                                    }

                                    AuthenticationMethod.APP_PASSWORD -> {
                                        AuthInput.AppPassword.from(requireNotNull(credential))
                                    }

                                    AuthenticationMethod.RECOVERY_CODE -> {
                                        AuthInput.RecoveryCode.from(requireNotNull(credential))
                                    }
                                },
                            )
                    ) {
                        is AuthenticationResult.Success -> {
                            resetInputState()
                        }

                        is AuthenticationResult.Cancelled -> {
                            Unit
                        }

                        is AuthenticationResult.Failure -> {
                            mutate(UnlockMutation.AuthenticationFailed(method, result.failure))
                            if (method != AuthenticationMethod.BIOMETRIC) refreshAttemptStatus(method)
                        }
                    }
                } finally {
                    credential?.let(MemoryCleaner::wipeCharArray)
                    clearCredential(method)
                    mutate(UnlockMutation.AuthenticationFinished)
                }
            }
        }

        private fun clearCredential(method: AuthenticationMethod) {
            when (method) {
                AuthenticationMethod.APP_PASSWORD -> {
                    _uiState.value.appPassword.wipe()
                    mutate(UnlockMutation.CredentialInputCleared(method))
                }

                AuthenticationMethod.RECOVERY_CODE -> {
                    _uiState.value.recoveryCode.wipe()
                    mutate(UnlockMutation.CredentialInputCleared(method))
                }

                AuthenticationMethod.BIOMETRIC -> {
                    Unit
                }
            }
        }

        private fun resetInputState() {
            recoveryRevealTapCount = 0
            wipeUnlockInputs()
            mutate(UnlockMutation.UnlockInputsReset)
        }

        private fun mutate(mutation: UnlockMutation) {
            _uiState.value = UnlockReducer.reduce(_uiState.value, mutation)
        }

        private fun refreshAttemptStatus(method: AuthenticationMethod) {
            val previous = attemptStatusJobs.remove(method)
            previous?.cancel()
            lateinit var job: Job
            job =
                viewModelScope.launch(start = CoroutineStart.LAZY) {
                    try {
                        var status = _uiState.value.authenticationAttemptStatuses[method]
                        try {
                            status = attemptStatusProvider.status(method)
                            mutate(UnlockMutation.AuthenticationAttemptStatusChanged(method, status))
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            if (status?.retryAfterMs == null) return@launch
                        }
                        while (status?.retryAfterMs != null) {
                            delay(1_000L)
                            status =
                                try {
                                    attemptStatusProvider.status(method)
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    continue
                                }
                            mutate(UnlockMutation.AuthenticationAttemptStatusChanged(method, status))
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } finally {
                        if (attemptStatusJobs[method] === job) attemptStatusJobs.remove(method)
                    }
                }
            attemptStatusJobs[method] = job
            job.start()
        }

        override fun onCleared() {
            attemptStatusJobs.values.forEach(Job::cancel)
            attemptStatusJobs.clear()
            wipeUnlockInputs()
            wipeSetupPasswords()
        }

        private fun wipeUnlockInputs() {
            _uiState.value.appPassword.wipe()
            _uiState.value.recoveryCode.wipe()
        }

        private fun wipeSetupPasswords() {
            val draft = editablePasswordDraft() ?: return
            draft.newPassword.wipe()
            draft.confirmPassword.wipe()
        }

        private fun editablePasswordDraft(): PasswordSetupDraft? =
            when (val setup = _uiState.value.passwordSetup) {
                is UnlockPasswordSetup.Editing -> setup.draft
                else -> null
            }

        private companion object {
            const val RECOVERY_REVEAL_TAP_THRESHOLD = 12
        }
    }
