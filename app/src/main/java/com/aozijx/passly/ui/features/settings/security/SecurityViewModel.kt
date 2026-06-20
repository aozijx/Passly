package com.aozijx.passly.ui.features.settings.security

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.auth.validation.AuthRequestValidator
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.usecase.auth.AuthUseCases
import com.aozijx.passly.domain.usecase.settings.security.SecuritySettingsUseCases
import com.aozijx.passly.ui.features.verification.internal.VerificationCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SecurityUiState(
    val lockTimeout: Long = 60_000L,
    val isPasswordPreferredAuthFirst: Boolean = true,
    val isDeviceCredentialFallbackEnabled: Boolean = true,
    val isInvalidateKeyOnBioChange: Boolean = true,
)

sealed interface SecurityUiAction {
    data class SetLockTimeout(val timeoutMs: Long) : SecurityUiAction
    data class SetPasswordPreferredAuthFirst(val enabled: Boolean) : SecurityUiAction
    data class ToggleDeviceCredentialFallback(val enabled: Boolean) : SecurityUiAction
}

@HiltViewModel
class SecurityViewModel @Inject constructor(
    authUseCases: AuthUseCases,
    authRequestValidator: AuthRequestValidator,
    private val securitySettingsUseCases: SecuritySettingsUseCases
) : ViewModel() {

    val authGateway = VerificationCoordinator(viewModelScope, authUseCases, authRequestValidator)

    val config: StateFlow<SecurityUiState> = combine(
        securitySettingsUseCases.lockTimeout,
        securitySettingsUseCases.isPasswordPreferredAuthFirst,
        securitySettingsUseCases.isDeviceCredentialFallbackEnabled,
        securitySettingsUseCases.isInvalidateKeyOnBioChange
    ) { lt, pfa, dcf, ibc ->
        SecurityUiState(
            lockTimeout = lt,
            isPasswordPreferredAuthFirst = pfa,
            isDeviceCredentialFallbackEnabled = dcf,
            isInvalidateKeyOnBioChange = ibc,
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

            is SecurityUiAction.SetPasswordPreferredAuthFirst -> viewModelScope.launch {
                securitySettingsUseCases.setPasswordPreferredAuthFirst(action.enabled)
            }

            is SecurityUiAction.ToggleDeviceCredentialFallback -> viewModelScope.launch {
                securitySettingsUseCases.setDeviceCredentialFallbackEnabled(action.enabled)
            }
        }
    }

    fun switchKeyInvalidationPolicy(
        activity: FragmentActivity,
        enabled: Boolean,
        onResult: (AppResult<Unit>) -> Unit
    ) {
        authGateway.rekeyWithInvalidationPolicy(activity, enabled, onResult)
    }
}