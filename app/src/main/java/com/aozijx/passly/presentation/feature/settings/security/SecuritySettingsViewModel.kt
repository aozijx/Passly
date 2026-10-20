package com.aozijx.passly.presentation.feature.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.settings.port.SecuritySettingsRepository
import com.aozijx.passly.feature.settings.security.SecurityAuthenticationSettingsInteractor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecuritySettingsViewModel @Inject constructor(
    private val authenticationSettings: SecurityAuthenticationSettingsInteractor,
    private val settingsRepository: SecuritySettingsRepository
) : ViewModel() {

    val uiState: StateFlow<SecuritySettingsUiState> = combine(
        settingsRepository.security,
        authenticationSettings.isBiometricEnabled,
    ) { security, isBiometricEnabled ->
        SecuritySettingsUiState.Ready(
            lockTimeout = security.lockTimeout,
            isInvalidateKeyOnBioChange = security.isInvalidateBiometricKeyOnChange,
            isLockOnBackground = security.isLockOnBackground,
            isBiometricEnabled = isBiometricEnabled,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = SecuritySettingsUiState.Loading,
    )

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

        }
    }

    private fun setKeyInvalidationPolicy(enabled: Boolean) {
        viewModelScope.launch {
            authenticationSettings.setKeyInvalidationPolicy(enabled)
        }
    }

    private fun setBiometricEnabled(enabled: Boolean) {
        val settings = uiState.value as? SecuritySettingsUiState.Ready ?: return
        viewModelScope.launch {
            authenticationSettings.setBiometricEnabled(
                enabled = enabled,
                invalidateOnEnrollment = settings.isInvalidateKeyOnBioChange,
            )
        }
    }
}
