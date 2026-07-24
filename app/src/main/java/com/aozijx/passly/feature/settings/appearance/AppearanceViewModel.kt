package com.aozijx.passly.feature.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.settings.command.SettingsCommand
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppearanceUiState(
    val isDarkMode: Boolean? = null,
    val isDynamicColor: Boolean = true,
    val themeColor: Long = 0,
)

sealed interface AppearanceUiAction {
    data class SetDarkMode(val enabled: Boolean?) : AppearanceUiAction
    data class SetDynamicColor(val enabled: Boolean) : AppearanceUiAction
    data class SetThemeColor(val color: Long) : AppearanceUiAction
}

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    val config: StateFlow<AppearanceUiState> = combine(
        settingsRepository.settings.map { it.appearance.isDarkMode },
        settingsRepository.settings.map { it.appearance.isDynamicColor },
        settingsRepository.settings.map { it.appearance.themeColor }
    ) { dm, dc, tc ->
        val themeColorLong = tc.toLongOrNull() ?: 0L
        AppearanceUiState(
            isDarkMode = dm,
            isDynamicColor = dc,
            themeColor = themeColorLong
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000L),
        AppearanceUiState()
    )

    fun onAction(action: AppearanceUiAction) {
        when (action) {
            is AppearanceUiAction.SetDarkMode -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetDarkMode(action.enabled))
            }

            is AppearanceUiAction.SetDynamicColor -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetDynamicColor(action.enabled))
            }

            is AppearanceUiAction.SetThemeColor -> viewModelScope.launch {
                val colorStr = if (action.color == 0L) "" else action.color.toString()
                settingsRepository.update(SettingsCommand.SetThemeColor(colorStr))
            }
        }
    }
}
