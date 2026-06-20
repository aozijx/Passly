package com.aozijx.passly.ui.features.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.AppDefaults
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
    val isDynamicColor: Boolean = AppDefaults.DISPLAY_DYNAMIC_COLOR,
)

sealed interface AppearanceUiAction {
    data class SetDarkMode(val enabled: Boolean?) : AppearanceUiAction
    data class SetDynamicColor(val enabled: Boolean) : AppearanceUiAction
}

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val systemSettingsUseCases: SystemSettingsUseCases
) : ViewModel() {

    val config: StateFlow<AppearanceUiState> = combine(
        systemSettingsUseCases.isDarkMode,
        systemSettingsUseCases.isDynamicColor
    ) { dm, dc ->
        AppearanceUiState(isDarkMode = dm, isDynamicColor = dc)
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
        }
    }
}