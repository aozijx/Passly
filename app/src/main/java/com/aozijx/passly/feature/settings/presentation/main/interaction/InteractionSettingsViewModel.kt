package com.aozijx.passly.feature.settings.presentation.main.interaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.R
import com.aozijx.passly.app.settings.RecoverableRead
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.domain.settings.model.InteractionSettings
import com.aozijx.passly.domain.settings.port.InteractionSettingsRepository
import com.aozijx.passly.presentation.shared.error.writePreferenceWithFeedback
import com.aozijx.passly.presentation.shared.event.PageEffects
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
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
                source = settingsRepository.interaction,
                scope = viewModelScope,
            )

        val uiState: StateFlow<RecoverableReadState<InteractionSettings>> = settingsRead.state

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
            if (uiState.value !is RecoverableReadState.Ready) return
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
