package com.aozijx.passly.presentation.feature.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.settings.port.AppearanceSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppearanceSettingsViewModel @Inject constructor(
    private val settingsRepository: AppearanceSettingsRepository
) : ViewModel() {

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
