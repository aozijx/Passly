package com.aozijx.passly.feature.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.AuthenticationMethodProvisioner
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.settings.model.SettingsCommand
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
import com.aozijx.passly.feature.settings.security.presentation.SecuritySettingsMutation
import com.aozijx.passly.feature.settings.security.presentation.SecuritySettingsReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecuritySettingsViewModel @Inject constructor(
    private val authenticationManager: AuthenticationManager,
    private val methodProvisioner: AuthenticationMethodProvisioner,
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecuritySettingsUiState())
    val uiState: StateFlow<SecuritySettingsUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
        observeAuthenticationMethods()
        loadRecoveryEnvelopeAvailability()
    }

    fun onAction(action: SecuritySettingsAction) {
        when (action) {
            is SecuritySettingsAction.SetLockTimeout -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetLockTimeout(action.timeoutMs))
            }

            is SecuritySettingsAction.ToggleLockOnBackground -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetLockOnBackground(action.enabled))
            }

            is SecuritySettingsAction.SetBiometricEnabled ->
                setBiometricEnabled(action.enabled)

            is SecuritySettingsAction.SetInvalidateKeyOnBiometricChange ->
                setKeyInvalidationPolicy(action.enabled)

            is SecuritySettingsAction.VerifyRecoveryCode -> viewModelScope.launch {
                if (isRecoveryMode()) {
                    action.code.fill('\u0000')
                    mutate(SecuritySettingsMutation.RecoveryCodeVerificationChanged(false))
                    return@launch
                }
                val valid = methodProvisioner.checkRecoveryCode(action.code)
                mutate(SecuritySettingsMutation.RecoveryCodeVerificationChanged(valid))
            }

            SecuritySettingsAction.ClearVerifyResult -> {
                mutate(SecuritySettingsMutation.RecoveryCodeVerificationChanged(null))
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                val security = settings.security
                mutate(
                    SecuritySettingsMutation.SettingsChanged(
                        lockTimeout = security.lockTimeout,
                        invalidateKeyOnBiometricChange =
                            security.isInvalidateBiometricKeyOnChange,
                        lockOnBackground = security.isLockOnBackground,
                    )
                )
            }
        }
    }

    private fun observeAuthenticationMethods() {
        viewModelScope.launch {
            authenticationManager.methods.collect { methods ->
                mutate(
                    SecuritySettingsMutation.BiometricAvailabilityChanged(AuthenticationMethod.BIOMETRIC in methods)
                )
            }
        }
    }

    private fun loadRecoveryEnvelopeAvailability() {
        viewModelScope.launch {
            mutate(
                SecuritySettingsMutation.RecoveryEnvelopeAvailabilityChanged(
                    methodProvisioner.hasRecoveryCode()
                )
            )
        }
    }

    private fun setKeyInvalidationPolicy(enabled: Boolean) {
        viewModelScope.launch {
            if (isRecoveryMode()) return@launch
            val result = methodProvisioner.rotateBiometricPolicy(enabled)
            if (result is AuthenticationResult.Success) {
                settingsRepository.update(SettingsCommand.SetInvalidateBiometricKeyOnChange(enabled))
            }
        }
    }

    private fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (isRecoveryMode()) return@launch
            if (enabled) {
                methodProvisioner.rotateBiometricPolicy(
                    uiState.value.isInvalidateKeyOnBioChange
                )
            } else {
                methodProvisioner.disableBiometric()
            }
        }
    }

    private fun mutate(mutation: SecuritySettingsMutation) {
        _uiState.update { state -> SecuritySettingsReducer.reduce(state, mutation) }
    }

    private fun isRecoveryMode(): Boolean =
        authenticationManager.state.value is AuthenticationState.RecoveryMode
}
