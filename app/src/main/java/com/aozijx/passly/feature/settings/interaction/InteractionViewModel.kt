package com.aozijx.passly.feature.settings.interaction

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.autofill.AutofillCoordinator
import com.aozijx.passly.domain.model.settings.AutofillUiMode
import com.aozijx.passly.domain.model.settings.SwipeActionType
import com.aozijx.passly.domain.usecase.settings.PortableSettingsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InteractionUiState(
    val isSwipeEnabled: Boolean = true,
    val swipeLeftAction: SwipeActionType = SwipeActionType.COPY_PASSWORD,
    val swipeRightAction: SwipeActionType = SwipeActionType.DETAIL,
    val autofillUiMode: AutofillUiMode = AutofillUiMode.SYSTEM_INLINE,
)

sealed interface InteractionUiAction {
    data class SetSwipeEnabled(val enabled: Boolean) : InteractionUiAction
    data class SetSwipeLeftAction(val action: SwipeActionType) : InteractionUiAction
    data class SetSwipeRightAction(val action: SwipeActionType) : InteractionUiAction
    data object ToggleAutofillUiMode : InteractionUiAction
}

@HiltViewModel
class InteractionViewModel @Inject constructor(
    private val application: android.app.Application,
    private val portableSettingsUseCases: PortableSettingsUseCases
) : AndroidViewModel(application) {

    val config: StateFlow<InteractionUiState> = combine(
        portableSettingsUseCases.isSwipeEnabled,
        portableSettingsUseCases.swipeLeftAction,
        portableSettingsUseCases.swipeRightAction,
        portableSettingsUseCases.autofillUiMode
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
                portableSettingsUseCases.setSwipeEnabled(action.enabled)
            }

            is InteractionUiAction.SetSwipeLeftAction -> viewModelScope.launch {
                portableSettingsUseCases.setSwipeLeftAction(action.action)
            }

            is InteractionUiAction.SetSwipeRightAction -> viewModelScope.launch {
                portableSettingsUseCases.setSwipeRightAction(action.action)
            }

            is InteractionUiAction.ToggleAutofillUiMode -> {
                val next = when (config.value.autofillUiMode) {
                    AutofillUiMode.SYSTEM_INLINE -> AutofillUiMode.BOTTOM_SHEET
                    AutofillUiMode.BOTTOM_SHEET -> AutofillUiMode.SYSTEM_INLINE
                }
                viewModelScope.launch { portableSettingsUseCases.setAutofillUiMode(next) }
            }
        }
    }

    fun openAutofillSettings() {
        AutofillCoordinator().requestEnable(getApplication())
    }
}