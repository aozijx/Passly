package com.aozijx.passly.presentation.feature.shell.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.port.AppearanceSettingsRepository
import com.aozijx.passly.domain.settings.port.InterfaceSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal sealed interface AppThemeSettingsState {
    data object Loading : AppThemeSettingsState

    data class Ready(
        val appearance: AppearanceSettings,
        val appCornerRadiusDp: Float,
    ) : AppThemeSettingsState
}

@HiltViewModel
internal class AppThemeSettingsViewModel @Inject constructor(
    appearanceSettingsRepository: AppearanceSettingsRepository,
    interfaceSettingsRepository: InterfaceSettingsRepository,
) : ViewModel() {

    val state: StateFlow<AppThemeSettingsState> = combine(
        appearanceSettingsRepository.appearance,
        interfaceSettingsRepository.interfaceSettings,
    ) { appearance, interfaceSettings ->
        AppThemeSettingsState.Ready(
            appearance = appearance,
            appCornerRadiusDp = interfaceSettings.appCornerRadiusDp,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppThemeSettingsState.Loading,
    )
}
