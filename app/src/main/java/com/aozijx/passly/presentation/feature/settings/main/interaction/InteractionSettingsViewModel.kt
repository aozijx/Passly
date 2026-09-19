package com.aozijx.passly.presentation.feature.settings.main.interaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.presentation.feature.common.error.toUiMessage
import com.aozijx.passly.domain.settings.port.InteractionSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class InteractionSettingsViewModel @Inject constructor(
    private val settingsRepository: InteractionSettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<InteractionSettingsUiState> = settingsRepository.interaction
        .map { settings ->
            InteractionSettingsUiState(
                isSwipeEnabled = settings.isSwipeEnabled,
                swipeLeftAction = settings.swipeLeftAction,
                swipeRightAction = settings.swipeRightAction,
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            InteractionSettingsUiState(),
        )

    private val _effects = Channel<InteractionSettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onAction(action: InteractionSettingsAction) {
        when (action) {
            is InteractionSettingsAction.SetSwipeEnabled -> viewModelScope.launch {
                settingsRepository.setSwipeEnabled(action.enabled)
            }
            is InteractionSettingsAction.SetSwipeLeftAction -> saveSwipeAction {
                settingsRepository.setSwipeLeftAction(action.action.toFeatureModel())
            }
            is InteractionSettingsAction.SetSwipeRightAction -> saveSwipeAction {
                settingsRepository.setSwipeRightAction(action.action.toFeatureModel())
            }
        }
    }

    private fun saveSwipeAction(save: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { save() }
                .onSuccess { _effects.trySend(InteractionSettingsEffect.Saved) }
                .onFailure { error ->
                    _effects.trySend(
                        InteractionSettingsEffect.SaveFailed(error.toUiMessage("保存失败")),
                    )
                }
        }
    }
}
