package com.aozijx.passly.feature.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.settings.model.SettingsCommand
import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppearanceSettingsViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    val config: StateFlow<AppearanceSettingsUiState> = settingsRepository.settings
        .map { it.appearance.toUiState() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            AppearanceSettingsUiState()
        )

    fun onAction(action: AppearanceSettingsAction) {
        when (action) {
            is AppearanceSettingsAction.SetThemeMode -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetThemeMode(action.mode))
            }

            is AppearanceSettingsAction.SetDynamicColor -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetDynamicColor(action.enabled))
            }

            is AppearanceSettingsAction.SetFallbackPalette -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetFallbackPalette(action.palette))
            }

            is AppearanceSettingsAction.SetLanguage -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetLanguage(action.language))
            }

            is AppearanceSettingsAction.SetFontFamily -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetFontFamily(action.mode))
            }
        }
    }
}

private fun AppearanceSettings.toUiState(): AppearanceSettingsUiState = AppearanceSettingsUiState(
    themeMode = themeMode,
    isDynamicColor = isDynamicColor,
    fallbackPalette = fallbackPalette,
    language = language,
    fontFamily = fontFamily
)
