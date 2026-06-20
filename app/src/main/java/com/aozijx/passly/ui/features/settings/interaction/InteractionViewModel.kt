package com.aozijx.passly.ui.features.settings.interaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.AppDefaults
import com.aozijx.passly.domain.model.AutofillUiMode
import com.aozijx.passly.domain.model.SwipeActionType
import com.aozijx.passly.domain.usecase.settings.system.SystemSettingsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InteractionUiState(
    val isSwipeEnabled: Boolean = AppDefaults.VAULT_SWIPE_ENABLED,
    val swipeLeftAction: SwipeActionType = AppDefaults.VAULT_SWIPE_LEFT_ACTION,
    val swipeRightAction: SwipeActionType = AppDefaults.VAULT_SWIPE_RIGHT_ACTION,
    val autofillUiMode: AutofillUiMode = AppDefaults.VAULT_AUTOFILL_UI_MODE,
)

sealed interface InteractionUiAction {
    data class SetSwipeEnabled(val enabled: Boolean) : InteractionUiAction
    data class SetSwipeLeftAction(val action: SwipeActionType) : InteractionUiAction
    data class SetSwipeRightAction(val action: SwipeActionType) : InteractionUiAction
    data object ToggleAutofillUiMode : InteractionUiAction
}

@HiltViewModel
class InteractionViewModel @Inject constructor(
    private val systemSettingsUseCases: SystemSettingsUseCases
) : ViewModel() {

    val config: StateFlow<InteractionUiState> = combine(
        systemSettingsUseCases.isSwipeEnabled,
        systemSettingsUseCases.swipeLeftAction,
        systemSettingsUseCases.swipeRightAction,
        systemSettingsUseCases.autofillUiMode
    ) { se, sl, sr, af ->
        InteractionUiState(
            isSwipeEnabled = se,
            swipeLeftAction = sl,
            swipeRightAction = sr,
            autofillUiMode = af,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000L),
        InteractionUiState()
    )

    fun onAction(action: InteractionUiAction) {
        when (action) {
            is InteractionUiAction.SetSwipeEnabled -> viewModelScope.launch {
                systemSettingsUseCases.setSwipeEnabled(action.enabled)
            }

            is InteractionUiAction.SetSwipeLeftAction -> viewModelScope.launch {
                systemSettingsUseCases.setSwipeLeftAction(action.action)
            }

            is InteractionUiAction.SetSwipeRightAction -> viewModelScope.launch {
                systemSettingsUseCases.setSwipeRightAction(action.action)
            }

            is InteractionUiAction.ToggleAutofillUiMode -> {
                val next = when (config.value.autofillUiMode) {
                    AutofillUiMode.SYSTEM_INLINE -> AutofillUiMode.BOTTOM_SHEET
                    AutofillUiMode.BOTTOM_SHEET -> AutofillUiMode.SYSTEM_INLINE
                }
                viewModelScope.launch { systemSettingsUseCases.setAutofillUiMode(next) }
            }
        }
    }
}