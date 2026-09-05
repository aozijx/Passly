package com.aozijx.passly.presentation.feature.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.settings.port.InterfaceSettingsRepository
import com.aozijx.passly.domain.settings.port.SecuritySettingsSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppShellSettingsViewModel @Inject constructor(
    securitySettingsSource: SecuritySettingsSource,
    interfaceSettingsRepository: InterfaceSettingsRepository,
) : ViewModel() {

    val config: StateFlow<AppShellSettingsUiState> = combine(
        securitySettingsSource.security,
        interfaceSettingsRepository.interfaceSettings,
    ) { security, interfaceSettings ->
        AppShellSettingsUiState(
            isSecureContentEnabled = security.isSecureContentEnabled,
            isFlipToLockEnabled = security.isFlipToLockEnabled,
            isFlipExitAndClearStackEnabled = security.isFlipExitAndClearStackEnabled,
            isStatusBarAutoHide = interfaceSettings.preferences.hideSystemBars,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000L),
        AppShellSettingsUiState()
    )
}
