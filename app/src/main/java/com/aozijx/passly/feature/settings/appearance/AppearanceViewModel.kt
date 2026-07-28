package com.aozijx.passly.feature.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.settings.command.SettingsCommand
import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.model.FallbackPalette
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.ThemeMode
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppearanceUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isDynamicColor: Boolean = true,
    val fallbackPalette: FallbackPalette = FallbackPalette.BLUE,
    val customSeedArgb: Long? = null,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val fontFamily: FontFamilyMode = FontFamilyMode.APP_BUNDLED,
    val isExpressive: Boolean = true
)

sealed interface AppearanceUiAction {
    data class SetThemeMode(val mode: ThemeMode) : AppearanceUiAction
    data class SetDynamicColor(val enabled: Boolean) : AppearanceUiAction
    data class SetFallbackPalette(val palette: FallbackPalette) : AppearanceUiAction
    data class SelectManualThemeColor(val argb: Long?) : AppearanceUiAction
    data class SetLanguage(val language: AppLanguage) : AppearanceUiAction
    data class SetFontFamily(val mode: FontFamilyMode) : AppearanceUiAction
    data class SetExpressiveEnabled(val enabled: Boolean) : AppearanceUiAction
}

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    val config: StateFlow<AppearanceUiState> = settingsRepository.settings
        .map { it.appearance.toUiState() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            AppearanceUiState()
        )

    fun onAction(action: AppearanceUiAction) {
        when (action) {
            is AppearanceUiAction.SetThemeMode -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetThemeMode(action.mode))
            }

            is AppearanceUiAction.SetDynamicColor -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetDynamicColor(action.enabled))
            }

            is AppearanceUiAction.SetFallbackPalette -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetFallbackPalette(action.palette))
            }

            is AppearanceUiAction.SelectManualThemeColor -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SelectManualThemeColor(action.argb))
            }

            is AppearanceUiAction.SetLanguage -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetLanguage(action.language))
            }

            is AppearanceUiAction.SetFontFamily -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetFontFamily(action.mode))
            }

            is AppearanceUiAction.SetExpressiveEnabled -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetExpressiveEnabled(action.enabled))
            }
        }
    }
}

private fun AppearanceSettings.toUiState(): AppearanceUiState = AppearanceUiState(
    themeMode = themeMode,
    isDynamicColor = isDynamicColor,
    fallbackPalette = fallbackPalette,
    customSeedArgb = customSeedArgb,
    language = language,
    fontFamily = fontFamily,
    isExpressive = isExpressive
)
