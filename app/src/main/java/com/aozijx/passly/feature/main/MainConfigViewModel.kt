package com.aozijx.passly.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.usecase.settings.DeviceSettingsUseCases
import com.aozijx.passly.domain.usecase.settings.PortableSettingsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    private val deviceSettingsUseCases: DeviceSettingsUseCases,
    private val portableSettingsUseCases: PortableSettingsUseCases
) : ViewModel() {

    val config: StateFlow<MainConfigUiState> = combine(
        deviceSettingsUseCases.isSecureContentEnabled,
        deviceSettingsUseCases.isFlipToLockEnabled,
        deviceSettingsUseCases.isFlipExitAndClearStackEnabled,
        portableSettingsUseCases.isStatusBarAutoHide
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
