package com.aozijx.passly.presentation.feature.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.settings.port.SecuritySettingsRepository
import com.aozijx.passly.feature.settings.security.SecurityAuthenticationSettingsInteractor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecuritySettingsViewModel @Inject constructor(
    private val authenticationSettings: SecurityAuthenticationSettingsInteractor,
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
                val valid = authenticationSettings.verifyRecoveryCode(action.code)
                _uiState.update { it.copy(recoveryCodeVerificationResult = valid) }
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
            authenticationSettings.isBiometricEnabled.collect { enabled ->
                _uiState.update {
                    it.copy(isBiometricEnabled = enabled)
                }
            }
        }
    }

    private fun loadRecoveryEnvelopeAvailability() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(hasRecoveryEnvelope = authenticationSettings.hasRecoveryCode())
            }
        }
    }

    private fun setKeyInvalidationPolicy(enabled: Boolean) {
        viewModelScope.launch {
            authenticationSettings.setKeyInvalidationPolicy(enabled)
        }
    }

    private fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            authenticationSettings.setBiometricEnabled(
                enabled = enabled,
                invalidateOnEnrollment = uiState.value.isInvalidateKeyOnBioChange,
            )
        }
    }
}
