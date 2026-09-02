package com.aozijx.passly.presentation.feature.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.port.AppearanceSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppearanceSettingsViewModel @Inject constructor(
    private val settingsRepository: AppearanceSettingsRepository
) : ViewModel() {

    val uiState: StateFlow<AppearanceSettingsUiState> = settingsRepository.appearance
        .map(AppearanceSettings::toUiState)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            AppearanceSettingsUiState()
        )

    fun onAction(action: AppearanceSettingsAction) {
        when (action) {
            is AppearanceSettingsAction.SetThemeMode -> viewModelScope.launch {
                settingsRepository.setThemeMode(action.mode)
            }

            is AppearanceSettingsAction.SetDynamicColor -> viewModelScope.launch {
                settingsRepository.setDynamicColor(action.enabled)
            }

            is AppearanceSettingsAction.SetThemeKey -> viewModelScope.launch {
                settingsRepository.setThemeKey(action.key)
            }

            is AppearanceSettingsAction.SetCanvasTintPercent -> viewModelScope.launch {
                settingsRepository.setCanvasTintPercent(action.percent)
            }

            is AppearanceSettingsAction.SetLanguage -> viewModelScope.launch {
                settingsRepository.setLanguage(action.language)
            }

            is AppearanceSettingsAction.SetFontFamily -> viewModelScope.launch {
                settingsRepository.setFontFamily(action.mode)
            }
        }
    }
}

private fun AppearanceSettings.toUiState(): AppearanceSettingsUiState = AppearanceSettingsUiState(
    themeMode = themeMode,
    isDynamicColor = isDynamicColor,
    themeKey = themeKey,
    canvasTintPercent = canvasTintPercent,
    language = language,
    fontFamily = fontFamily
)
