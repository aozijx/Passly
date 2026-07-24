package com.aozijx.passly.feature.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.authentication.AuthenticationManager
import com.aozijx.passly.domain.authentication.AuthenticationMethodProvisioner
import com.aozijx.passly.domain.authentication.AuthenticationResult
import com.aozijx.passly.domain.command.settings.SettingsCommand
import com.aozijx.passly.domain.repository.settings.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SecurityUiState(
    val lockTimeout: Long = 60_000L,
    val isInvalidateKeyOnBioChange: Boolean = true,
    val isLockOnBackground: Boolean = false,
    val clipboardClearToastsEnabled: Boolean = true,
    val appCloseToastsEnabled: Boolean = true
)

sealed interface SecurityUiAction {
    data class SetLockTimeout(val timeoutMs: Long) : SecurityUiAction
    data class ToggleLockOnBackground(val enabled: Boolean) : SecurityUiAction
    data class ToggleClipboardClearToasts(val enabled: Boolean) : SecurityUiAction
    data class ToggleAppCloseToasts(val enabled: Boolean) : SecurityUiAction
    data class VerifyRecoveryCode(val code: String) : SecurityUiAction
    data object ClearVerifyResult : SecurityUiAction
}

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val authenticationManager: AuthenticationManager,
    private val methodProvisioner: AuthenticationMethodProvisioner,
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    val config: StateFlow<SecurityUiState> = combine(
        settingsRepository.lockTimeout,
        settingsRepository.settings.map { it.security.isInvalidateKeyOnBioChange },
        settingsRepository.isLockOnBackground,
        settingsRepository.settings.map { it.notifications.clipboardClearToastsEnabled },
        settingsRepository.settings.map { it.notifications.appCloseToastsEnabled }
    ) { lt, ibc, lob, clipboardToasts, closeToasts ->
        SecurityUiState(
            lockTimeout = lt,
            isInvalidateKeyOnBioChange = ibc,
            isLockOnBackground = lob,
            clipboardClearToastsEnabled = clipboardToasts,
            appCloseToastsEnabled = closeToasts
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000L),
        SecurityUiState()
    )

    val isAppPasswordEnabled: StateFlow<Boolean> = authenticationManager.methods
        .map { it.appPassword }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isBiometricEnabled: StateFlow<Boolean> = authenticationManager.methods
        .map { it.biometric }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _hasRecoveryEnvelope = MutableStateFlow(false)
    val hasRecoveryEnvelope: StateFlow<Boolean> = _hasRecoveryEnvelope.asStateFlow()

    private val _verifyResult = MutableStateFlow<Boolean?>(null)
    val verifyResult: StateFlow<Boolean?> = _verifyResult.asStateFlow()

    fun onAction(action: SecurityUiAction) {
        when (action) {
            is SecurityUiAction.SetLockTimeout -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetLockTimeout(action.timeoutMs))
            }

            is SecurityUiAction.ToggleLockOnBackground -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetLockOnBackground(action.enabled))
            }

            is SecurityUiAction.ToggleClipboardClearToasts -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetClipboardClearToastsEnabled(action.enabled))
            }

            is SecurityUiAction.ToggleAppCloseToasts -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetAppCloseToastsEnabled(action.enabled))
            }

            is SecurityUiAction.VerifyRecoveryCode -> viewModelScope.launch {
                val valid = methodProvisioner.verifyRecoveryCode(action.code.toCharArray())
                _verifyResult.value = valid
            }

            SecurityUiAction.ClearVerifyResult -> {
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
            val result = methodProvisioner.rotateBiometricPolicy(enabled)
            if (result is AuthenticationResult.Success) {
                settingsRepository.update(SettingsCommand.SetInvalidateKeyOnBioChange(enabled))
            }
            onResult(result is AuthenticationResult.Success)
        }
    }

    fun setBiometricEnabled(
        enabled: Boolean,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val result = if (enabled) {
                methodProvisioner.rotateBiometricPolicy(config.value.isInvalidateKeyOnBioChange)
            } else {
                methodProvisioner.disableBiometric()
            }
            onResult(result is AuthenticationResult.Success)
        }
    }
}
