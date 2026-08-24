package com.aozijx.passly.presentation.feature.settings.main.interaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.settings.model.SettingsCommand
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InteractionSettingsViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<InteractionSettingsUiState> = settingsRepository.settings
        .map { settings ->
            InteractionSettingsUiState(
                isSwipeEnabled = settings.interaction.isSwipeEnabled,
                swipeLeftAction = settings.interaction.swipeLeftAction,
                swipeRightAction = settings.interaction.swipeRightAction,
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            InteractionSettingsUiState(),
        )

    fun onAction(action: InteractionSettingsAction) {
        val command = when (action) {
            is InteractionSettingsAction.SetSwipeEnabled ->
                SettingsCommand.SetSwipeEnabled(action.enabled)
        }
        viewModelScope.launch { settingsRepository.update(command) }
    }
}
