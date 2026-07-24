package com.aozijx.passly.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.repository.settings.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MainConfigUiState(
    val isSecureContentEnabled: Boolean = true,
    val isFlipToLockEnabled: Boolean = false,
    val isFlipExitAndClearStackEnabled: Boolean = false,
    val isStatusBarAutoHide: Boolean = true,
)

@HiltViewModel
class MainConfigViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    val config: StateFlow<MainConfigUiState> = combine(
        settingsRepository.settings.map { it.security.isSecureContentEnabled },
        settingsRepository.settings.map { it.security.isFlipToLockEnabled },
        settingsRepository.settings.map { it.security.isFlipExitAndClearStackEnabled },
        settingsRepository.settings.map { it.appearance.isStatusBarAutoHide }
    ) { sec, ftl, fec, sb ->
        MainConfigUiState(
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
