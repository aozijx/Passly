package com.aozijx.passly.presentation.feature.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.access.port.AuthenticationMethodAvailability
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.access.port.AuthenticationMethodProvisioner
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.settings.port.SecuritySettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecuritySettingsViewModel @Inject constructor(
    private val authenticationMethodAvailability: AuthenticationMethodAvailability,
    private val secureSessionAccessState: SecureSessionAccessState,
    private val methodProvisioner: AuthenticationMethodProvisioner,
    private val settingsRepository: SecuritySettingsRepository
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
                settingsRepository.setLockTimeout(action.timeoutMs)
            }

            is SecuritySettingsAction.ToggleLockOnBackground -> viewModelScope.launch {
                settingsRepository.setLockOnBackground(action.enabled)
            }

            is SecuritySettingsAction.SetBiometricEnabled ->
                setBiometricEnabled(action.enabled)

            is SecuritySettingsAction.SetInvalidateKeyOnBiometricChange ->
                setKeyInvalidationPolicy(action.enabled)

            is SecuritySettingsAction.VerifyRecoveryCode -> viewModelScope.launch {
                try {
                    if (isRecoveryMode()) {
                        _uiState.update { it.copy(recoveryCodeVerificationResult = false) }
                        return@launch
                    }
                    val valid = methodProvisioner.checkRecoveryCode(action.code)
                    _uiState.update { it.copy(recoveryCodeVerificationResult = valid) }
                } finally {
                    action.code.fill('\u0000')
                }
            }

            SecuritySettingsAction.ClearVerifyResult -> {
                _uiState.update { it.copy(recoveryCodeVerificationResult = null) }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.security.collect { security ->
                _uiState.update {
                    it.copy(
                        lockTimeout = security.lockTimeout,
                        isInvalidateKeyOnBioChange = security.isInvalidateBiometricKeyOnChange,
                        isLockOnBackground = security.isLockOnBackground,
                    )
                }
            }
        }
    }

    private fun observeAuthenticationMethods() {
        viewModelScope.launch {
            authenticationMethodAvailability.methods.collect { methods ->
                _uiState.update {
                    it.copy(isBiometricEnabled = AuthenticationMethod.BIOMETRIC in methods)
                }
            }
        }
    }

    private fun loadRecoveryEnvelopeAvailability() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(hasRecoveryEnvelope = methodProvisioner.hasRecoveryCode())
            }
        }
    }

    private fun setKeyInvalidationPolicy(enabled: Boolean) {
        viewModelScope.launch {
            if (isRecoveryMode()) return@launch
            val result = methodProvisioner.rotateBiometricPolicy(enabled)
            if (result is AuthenticationResult.Success) {
                settingsRepository.setInvalidateBiometricKeyOnChange(enabled)
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

    private fun isRecoveryMode(): Boolean =
        secureSessionAccessState.isRecoveryMode()
}
