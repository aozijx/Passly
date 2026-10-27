package com.aozijx.passly.feature.settings.presentation.main.interaction

import com.aozijx.passly.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.presentation.shared.error.toUiErrorMessage
import com.aozijx.passly.domain.settings.port.InteractionSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CancellationException
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
            InteractionSettingsUiState.Ready(
                isSwipeEnabled = settings.isSwipeEnabled,
                isPullToSearchEnabled = settings.isPullToSearchEnabled,
                swipeLeftAction = settings.swipeLeftAction,
                swipeRightAction = settings.swipeRightAction,
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            InteractionSettingsUiState.Loading,
        )

    private val _effects = Channel<InteractionSettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onAction(action: InteractionSettingsAction) {
        when (action) {
            is InteractionSettingsAction.SetSwipeEnabled -> viewModelScope.launch {
                settingsRepository.setSwipeEnabled(action.enabled)
            }
            is InteractionSettingsAction.SetPullToSearchEnabled -> viewModelScope.launch {
                settingsRepository.setPullToSearchEnabled(action.enabled)
            }
            is InteractionSettingsAction.SetSwipeLeftAction -> saveSwipeAction {
                settingsRepository.setSwipeLeftAction(action.action)
            }
            is InteractionSettingsAction.SetSwipeRightAction -> saveSwipeAction {
                settingsRepository.setSwipeRightAction(action.action)
            }
        }
    }

    private fun saveSwipeAction(save: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                save()
                _effects.trySend(InteractionSettingsEffect.Saved)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _effects.trySend(
                    InteractionSettingsEffect.SaveFailed(error.toUiErrorMessage(R.string.settings_interaction_save_failed)),
                )
            }
        }
    }
}
