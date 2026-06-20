package com.aozijx.passly.ui.features.main.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.usecase.settings.security.SecuritySettingsUseCases
import com.aozijx.passly.domain.usecase.settings.system.SystemSettingsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MainConfigUiState(
    val isPasswordPreferredAuthFirst: Boolean = true,
    val isSecureContentEnabled: Boolean = true,
    val isFlipToLockEnabled: Boolean = false,
    val isFlipExitAndClearStackEnabled: Boolean = false,
    val isStatusBarAutoHide: Boolean = true,
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