package com.aozijx.passly.app.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppShellSettingsViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    val config: StateFlow<AppShellSettingsUiState> = combine(
        settingsRepository.settings.map { it.security.isSecureContentEnabled },
        settingsRepository.settings.map { it.security.isFlipToLockEnabled },
        settingsRepository.settings.map { it.security.isFlipExitAndClearStackEnabled },
        settingsRepository.settings.map { it.interfacePrefs.hideSystemBars }
    ) { sec, ftl, fec, sb ->
        AppShellSettingsUiState(
            isSecureContentEnabled = sec,
            isFlipToLockEnabled = ftl,
            isFlipExitAndClearStackEnabled = fec,
            isStatusBarAutoHide = sb,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000L),
        AppShellSettingsUiState()
    )
}
