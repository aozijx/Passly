package com.aozijx.passly.feature.settings.presentation.main.interaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.R
import com.aozijx.passly.app.settings.ReadFailure
import com.aozijx.passly.app.settings.RecoverableRead
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.domain.settings.port.InteractionSettingsRepository
import com.aozijx.passly.presentation.shared.error.writePreferenceWithFeedback
import com.aozijx.passly.presentation.shared.event.PageEffects
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class InteractionSettingsViewModel
    @Inject
    constructor(
        private val settingsRepository: InteractionSettingsRepository,
    ) : ViewModel() {
        private enum class SuccessFeedback { Silent, Saved }

        private val settingsRead =
            RecoverableRead(
                source =
                    settingsRepository.interaction
                        .map { settings ->
                            InteractionSettingsUiState.Ready(
                                isSwipeEnabled = settings.isSwipeEnabled,
                                isPullToSearchEnabled = settings.isPullToSearchEnabled,
                                swipeLeftAction = settings.swipeLeftAction,
                                swipeRightAction = settings.swipeRightAction,
                            )
                        },
                scope = viewModelScope,
            )

        val uiState: StateFlow<InteractionSettingsUiState> =
            settingsRead.state
                .map { state ->
                    when (state) {
                        RecoverableReadState.Loading -> InteractionSettingsUiState.Loading
                        is RecoverableReadState.Ready -> state.value
                        is RecoverableReadState.Failed -> InteractionSettingsUiState.Failed(state.failure)
                    }
                }.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000L),
                    InteractionSettingsUiState.Loading,
                )

        private val pageEffects = PageEffects<InteractionSettingsEffect>()
        val effects = pageEffects.effects

        fun onAction(action: InteractionSettingsAction) {
            when (action) {
                InteractionSettingsAction.RetryRead -> {
                    settingsRead.retry()
                }

                is InteractionSettingsAction.SetSwipeEnabled -> {
                    saveSetting(SuccessFeedback.Silent) {
                        settingsRepository.setSwipeEnabled(action.enabled)
                    }
                }

                is InteractionSettingsAction.SetPullToSearchEnabled -> {
                    saveSetting(SuccessFeedback.Silent) {
                        settingsRepository.setPullToSearchEnabled(action.enabled)
                    }
                }

                is InteractionSettingsAction.SetSwipeLeftAction -> {
                    saveSetting(SuccessFeedback.Saved) {
                        settingsRepository.setSwipeLeftAction(action.action)
                    }
                }

                is InteractionSettingsAction.SetSwipeRightAction -> {
                    saveSetting(SuccessFeedback.Saved) {
                        settingsRepository.setSwipeRightAction(action.action)
                    }
                }
            }
        }

        private fun saveSetting(
            feedback: SuccessFeedback,
            save: suspend () -> Unit,
        ) {
            if (uiState.value !is InteractionSettingsUiState.Ready) return
            val owner = pageEffects.currentOwner ?: return
            viewModelScope.launch {
                writePreferenceWithFeedback(
                    failureRes = R.string.settings_interaction_save_failed,
                    onFailure = {
                        pageEffects.tryEmit(owner, InteractionSettingsEffect.SaveFailed(it))
                    },
                    onSuccess = {
                        if (feedback == SuccessFeedback.Saved) {
                            pageEffects.tryEmit(owner, InteractionSettingsEffect.Saved)
                        }
                    },
                    write = save,
                )
            }
        }

        fun onPageEntered(): Any = pageEffects.enter()

        fun onPageExited(owner: Any) {
            pageEffects.exit(owner)
        }
    }
