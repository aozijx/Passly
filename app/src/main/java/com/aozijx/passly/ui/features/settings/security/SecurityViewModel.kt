package com.aozijx.passly.ui.features.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.auth.VerificationGateway
import com.aozijx.passly.core.auth.biometric.BiometricPromptLauncher
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.AppDefaults
import com.aozijx.passly.domain.usecase.settings.security.SecuritySettingsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SecurityUiState(
    val lockTimeout: Long = AppDefaults.Lock.DEFAULT_TIMEOUT_MS,
    val isInvalidateKeyOnBioChange: Boolean = AppDefaults.Security.INVALIDATE_KEY_ON_BIO_CHANGE,
    val isLockOnBackground: Boolean = AppDefaults.Security.LOCK_ON_BACKGROUND,
)

sealed interface SecurityUiAction {
    data class SetLockTimeout(val timeoutMs: Long) : SecurityUiAction
    data class ToggleLockOnBackground(val enabled: Boolean) : SecurityUiAction
}

@HiltViewModel
class SecurityViewModel @Inject constructor(
    val authGateway: VerificationGateway,
    private val securitySettingsUseCases: SecuritySettingsUseCases
) : ViewModel() {

    val config: StateFlow<SecurityUiState> = combine(
        securitySettingsUseCases.lockTimeout,
        securitySettingsUseCases.isInvalidateKeyOnBioChange,
        securitySettingsUseCases.isLockOnBackground
    ) { lt, ibc, lob ->
        SecurityUiState(
            lockTimeout = lt,
            isInvalidateKeyOnBioChange = ibc,
            isLockOnBackground = lob,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000L),
        SecurityUiState()
    )

    val isAppPasswordEnabled: StateFlow<Boolean> = authGateway.isAppPasswordEnabled

    fun onAction(action: SecurityUiAction) {
        when (action) {
            is SecurityUiAction.SetLockTimeout -> viewModelScope.launch {
                securitySettingsUseCases.setLockTimeout(action.timeoutMs)
            }

            is SecurityUiAction.ToggleLockOnBackground -> viewModelScope.launch {
                securitySettingsUseCases.setLockOnBackground(action.enabled)
            }
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