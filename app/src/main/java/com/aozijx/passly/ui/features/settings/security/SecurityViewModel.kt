package com.aozijx.passly.ui.features.settings.security

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.auth.authconstants.AuthLockConstants
import com.aozijx.passly.core.auth.validation.AuthRequestValidator
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.AppDefaults
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
    val lockTimeout: Long = AuthLockConstants.DEFAULT_LOCK_TIMEOUT_MS,
    val isInvalidateKeyOnBioChange: Boolean = AppDefaults.SECURITY_INVALIDATE_KEY_ON_BIO_CHANGE,
    val isLockOnBackground: Boolean = AppDefaults.SECURITY_LOCK_ON_BACKGROUND,
)

sealed interface SecurityUiAction {
    data class SetLockTimeout(val timeoutMs: Long) : SecurityUiAction
    data class ToggleLockOnBackground(val enabled: Boolean) : SecurityUiAction
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
        activity: FragmentActivity,
        enabled: Boolean,
        onResult: (AppResult<Unit>) -> Unit
    ) {
        authGateway.rekeyWithInvalidationPolicy(activity, enabled, onResult)
    }
}