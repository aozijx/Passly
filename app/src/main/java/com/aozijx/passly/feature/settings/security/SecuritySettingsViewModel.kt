package com.aozijx.passly.feature.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.authentication.AuthenticationManager
import com.aozijx.passly.domain.authentication.AuthenticationState
import com.aozijx.passly.domain.authentication.AuthenticationMethodProvisioner
import com.aozijx.passly.domain.authentication.AuthenticationResult
import com.aozijx.passly.domain.settings.command.SettingsCommand
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecuritySettingsViewModel @Inject constructor(
    private val authenticationManager: AuthenticationManager,
    private val methodProvisioner: AuthenticationMethodProvisioner,
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    val config: StateFlow<SecuritySettingsUiState> = settingsRepository.settings.map { settings ->
        val security = settings.security
        SecuritySettingsUiState(
            lockTimeout = security.lockTimeout,
            isInvalidateKeyOnBioChange = security.isInvalidateBiometricKeyOnChange,
            isLockOnBackground = security.isLockOnBackground
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000L),
        SecuritySettingsUiState()
    )

    val isBiometricEnabled: StateFlow<Boolean> = authenticationManager.methods
        .map { it.biometric }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _hasRecoveryEnvelope = MutableStateFlow(false)
    val hasRecoveryEnvelope: StateFlow<Boolean> = _hasRecoveryEnvelope.asStateFlow()

    private val _verifyResult = MutableStateFlow<Boolean?>(null)
    val verifyResult: StateFlow<Boolean?> = _verifyResult.asStateFlow()

    fun onAction(action: SecuritySettingsAction) {
        when (action) {
            is SecuritySettingsAction.SetLockTimeout -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetLockTimeout(action.timeoutMs))
            }

            is SecuritySettingsAction.ToggleLockOnBackground -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetLockOnBackground(action.enabled))
            }

            is SecuritySettingsAction.VerifyRecoveryCode -> viewModelScope.launch {
                if (isRecoveryMode()) {
                    _verifyResult.value = false
                    return@launch
                }
                val valid = methodProvisioner.checkRecoveryCode(action.code.toCharArray())
                _verifyResult.value = valid
            }

            SecuritySettingsAction.ClearVerifyResult -> {
                _verifyResult.value = null
            }

        }
    }

    init {
        viewModelScope.launch {
            _hasRecoveryEnvelope.value = methodProvisioner.hasRecoveryCode()
        }
    }

    fun switchKeyInvalidationPolicy(
        enabled: Boolean,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            if (isRecoveryMode()) return@launch onResult(false)
            val result = methodProvisioner.rotateBiometricPolicy(enabled)
            if (result is AuthenticationResult.Success) {
                settingsRepository.update(SettingsCommand.SetInvalidateBiometricKeyOnChange(enabled))
            }
            onResult(result is AuthenticationResult.Success)
        }
    }

    fun setBiometricEnabled(
        enabled: Boolean,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            if (isRecoveryMode()) return@launch onResult(false)
            val result = if (enabled) {
                methodProvisioner.rotateBiometricPolicy(config.value.isInvalidateKeyOnBioChange)
            } else {
                methodProvisioner.disableBiometric()
            }
            onResult(result is AuthenticationResult.Success)
        }
    }

    private fun isRecoveryMode(): Boolean =
        authenticationManager.state.value is AuthenticationState.RecoveryMode
}
