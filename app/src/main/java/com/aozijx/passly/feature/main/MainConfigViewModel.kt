package com.aozijx.passly.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.repository.settings.DeviceRepository
import com.aozijx.passly.domain.repository.settings.PortableRepository
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
    private val deviceRepository: DeviceRepository,
    private val portableRepository: PortableRepository
) : ViewModel() {

    val config: StateFlow<MainConfigUiState> = combine(
        deviceRepository.isSecureContentEnabled,
        deviceRepository.isFlipToLockEnabled,
        deviceRepository.isFlipExitAndClearStackEnabled,
        portableRepository.isStatusBarAutoHide
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
