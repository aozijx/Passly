package com.aozijx.passly.ui.features.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.model.AppDefaults
import com.aozijx.passly.domain.usecase.settings.system.SystemSettingsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppearanceUiState(
    val isDarkMode: Boolean? = null,
    val isDynamicColor: Boolean = AppDefaults.Display.DYNAMIC_COLOR,
    val languageCode: String = "",
    val themeColor: Long = 0,
)

sealed interface AppearanceUiAction {
    data class SetDarkMode(val enabled: Boolean?) : AppearanceUiAction
    data class SetDynamicColor(val enabled: Boolean) : AppearanceUiAction
    data class SetLanguageCode(val code: String) : AppearanceUiAction
    data class SetThemeColor(val color: Long) : AppearanceUiAction
}

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val systemSettingsUseCases: SystemSettingsUseCases
) : ViewModel() {

    val config: StateFlow<AppearanceUiState> = combine(
        systemSettingsUseCases.isDarkMode,
        systemSettingsUseCases.isDynamicColor,
        systemSettingsUseCases.languageCode,
        systemSettingsUseCases.themeColor
    ) { dm, dc, lang, tc ->
        val themeColorLong = tc.toLongOrNull() ?: 0L
        AppearanceUiState(isDarkMode = dm, isDynamicColor = dc, languageCode = lang, themeColor = themeColorLong)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000L),
        AppearanceUiState()
    )

    fun onAction(action: AppearanceUiAction) {
        when (action) {
            is AppearanceUiAction.SetDarkMode -> viewModelScope.launch {
                systemSettingsUseCases.setDarkMode(action.enabled)
            }

            is AppearanceUiAction.SetDynamicColor -> viewModelScope.launch {
                systemSettingsUseCases.setDynamicColor(action.enabled)
            }

            is AppearanceUiAction.SetLanguageCode -> viewModelScope.launch {
                systemSettingsUseCases.setLanguageCode(action.code)
            }

            is AppearanceUiAction.SetThemeColor -> viewModelScope.launch {
                val colorStr = if (action.color == 0L) "" else action.color.toString()
                systemSettingsUseCases.setThemeColor(colorStr)
            }
        }
    }
}