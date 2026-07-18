package com.aozijx.passly.feature.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.authentication.AuthenticationMethodProvisioner
import com.aozijx.passly.domain.usecase.settings.DeviceSettingsUseCases
import com.aozijx.passly.domain.usecase.settings.PortableSettingsUseCases
import com.aozijx.passly.domain.authentication.AuthenticationManager
import com.aozijx.passly.domain.authentication.AuthenticationPurpose
import com.aozijx.passly.domain.authentication.AuthenticationRequest
import com.aozijx.passly.domain.authentication.AuthenticationResult
import com.aozijx.passly.security.authentication.BiometricRotationCoordinator
import com.aozijx.passly.security.authentication.host.AuthenticationHostRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
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
    private val hostRegistry: AuthenticationHostRegistry,
    private val biometricRotationCoordinator: BiometricRotationCoordinator,
    private val methodProvisioner: AuthenticationMethodProvisioner,
    private val deviceSettingsUseCases: DeviceSettingsUseCases,
    private val portableSettingsUseCases: PortableSettingsUseCases
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

    val isAppPasswordEnabled: StateFlow<Boolean> = authenticationManager.methods
        .map { it.appPassword }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

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
            val authentication = authenticationManager.authenticate(
                AuthenticationRequest(AuthenticationPurpose.CHANGE_BIOMETRIC_POLICY)
            )
            if (authentication !is AuthenticationResult.Success) {
                onResult(false)
                return@launch
            }
            val host = hostRegistry.awaitLease()?.hostOrNull()
            if (host == null) {
                onResult(false)
                return@launch
            }
            val result = biometricRotationCoordinator.rotate(
                host = host,
                invalidateOnEnrollment = enabled,
                correlationId = java.util.UUID.randomUUID().toString()
            )
            onResult(result is AuthenticationResult.Success)
        }
    }
}
