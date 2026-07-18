package com.aozijx.passly.feature.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.usecase.auth.RecoveryCodeUseCases
import com.aozijx.passly.domain.usecase.settings.DeviceSettingsUseCases
import com.aozijx.passly.domain.usecase.settings.PortableSettingsUseCases
import com.aozijx.passly.feature.auth.VerificationGateway
import com.aozijx.passly.feature.auth.biometric.BiometricPromptLauncher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    data object CreateRecoveryCode : SecurityUiAction
    data object RegenerateRecoveryCode : SecurityUiAction
    data class VerifyRecoveryCode(val code: String) : SecurityUiAction
    data object ClearVerifyResult : SecurityUiAction
    data object DismissRecoveryCode : SecurityUiAction
}

@HiltViewModel
class SecurityViewModel @Inject constructor(
    val authGateway: VerificationGateway,
    private val deviceSettingsUseCases: DeviceSettingsUseCases,
    private val portableSettingsUseCases: PortableSettingsUseCases,
    private val recoveryCodeUseCases: RecoveryCodeUseCases
) : ViewModel() {

    val config: StateFlow<SecurityUiState> = combine(
        deviceSettingsUseCases.lockTimeout,
        deviceSettingsUseCases.isInvalidateKeyOnBioChange,
        deviceSettingsUseCases.isLockOnBackground,
        portableSettingsUseCases.clipboardClearToastsEnabled,
        portableSettingsUseCases.appCloseToastsEnabled
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

    val isAppPasswordEnabled: StateFlow<Boolean> = authGateway.isAppPasswordEnabled

    private val _recoveryCode = MutableStateFlow<String?>(value = null)
    val recoveryCode: StateFlow<String?> = _recoveryCode.asStateFlow()

    private val _hasRecoveryEnvelope = MutableStateFlow(false)
    val hasRecoveryEnvelope: StateFlow<Boolean> = _hasRecoveryEnvelope.asStateFlow()

    private val _verifyResult = MutableStateFlow<Boolean?>(null)
    val verifyResult: StateFlow<Boolean?> = _verifyResult.asStateFlow()

    fun onAction(action: SecurityUiAction) {
        when (action) {
            is SecurityUiAction.SetLockTimeout -> viewModelScope.launch {
                deviceSettingsUseCases.setLockTimeout(action.timeoutMs)
            }

            is SecurityUiAction.ToggleLockOnBackground -> viewModelScope.launch {
                deviceSettingsUseCases.setLockOnBackground(action.enabled)
            }

            is SecurityUiAction.ToggleClipboardClearToasts -> viewModelScope.launch {
                portableSettingsUseCases.setClipboardClearToastsEnabled(action.enabled)
            }

            is SecurityUiAction.ToggleAppCloseToasts -> viewModelScope.launch {
                portableSettingsUseCases.setAppCloseToastsEnabled(action.enabled)
            }

            SecurityUiAction.CreateRecoveryCode -> viewModelScope.launch {
                val code = recoveryCodeUseCases.create()
                _recoveryCode.value = String(code)
                code.fill('\u0000')
                _hasRecoveryEnvelope.value = true
            }

            SecurityUiAction.RegenerateRecoveryCode -> viewModelScope.launch {
                val code = recoveryCodeUseCases.regenerate()
                _hasRecoveryEnvelope.value = true
                _recoveryCode.value = String(code)
                code.fill('\u0000')
            }

            is SecurityUiAction.VerifyRecoveryCode -> viewModelScope.launch {
                val valid = recoveryCodeUseCases.verify(action.code.toCharArray())
                _verifyResult.value = valid
            }

            SecurityUiAction.ClearVerifyResult -> {
                _verifyResult.value = null
            }

            SecurityUiAction.DismissRecoveryCode -> {
                _recoveryCode.value = null
            }
        }
    }

    init {
        viewModelScope.launch {
            _hasRecoveryEnvelope.value = recoveryCodeUseCases.hasRecoveryCode()
        }
    }

    fun switchKeyInvalidationPolicy(
        launcher: BiometricPromptLauncher,
        enabled: Boolean,
        onResult: (AppResult<Unit>) -> Unit
    ) {
        authGateway.rekeyWithInvalidationPolicy(launcher, enabled, onResult)
    }
}
