package com.aozijx.passly.ui.features.main.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.AppDefaults
import com.aozijx.passly.domain.usecase.settings.security.SecuritySettingsUseCases
import com.aozijx.passly.domain.usecase.settings.system.SystemSettingsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MainConfigUiState(
    val isPasswordPreferredAuthFirst: Boolean = AppDefaults.SECURITY_PASSWORD_PREFERRED_AUTH_FIRST,
    val isSecureContentEnabled: Boolean = AppDefaults.SECURITY_SECURE_CONTENT_ENABLED,
    val isFlipToLockEnabled: Boolean = AppDefaults.SECURITY_FLIP_TO_LOCK_ENABLED,
    val isFlipExitAndClearStackEnabled: Boolean = AppDefaults.SECURITY_FLIP_EXIT_AND_CLEAR_STACK_ENABLED,
    val isStatusBarAutoHide: Boolean = AppDefaults.DISPLAY_STATUS_BAR_AUTO_HIDE,
)

@HiltViewModel
class MainConfigViewModel @Inject constructor(
    private val securitySettingsUseCases: SecuritySettingsUseCases,
    private val systemSettingsUseCases: SystemSettingsUseCases
) : ViewModel() {

    val config: StateFlow<MainConfigUiState> = combine(
        securitySettingsUseCases.isPasswordPreferredAuthFirst,
        securitySettingsUseCases.isSecureContentEnabled,
        securitySettingsUseCases.isFlipToLockEnabled,
        securitySettingsUseCases.isFlipExitAndClearStackEnabled,
        systemSettingsUseCases.isStatusBarAutoHide
    ) { pfa, sec, ftl, fec, sb ->
        MainConfigUiState(
            isPasswordPreferredAuthFirst = pfa,
            isSecureContentEnabled = sec,
            isFlipToLockEnabled = ftl,
            isFlipExitAndClearStackEnabled = fec,
            isStatusBarAutoHide = sb,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000L),
        MainConfigUiState()
    )
}