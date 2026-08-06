package com.aozijx.passly.feature.settings.interaction

import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.domain.settings.model.SwipeActionType

data class InteractionSettingsUiState(
    val isSwipeEnabled: Boolean = false,
    val swipeLeftAction: SwipeActionType = SwipeActionType.COPY_PASSWORD,
    val swipeRightAction: SwipeActionType = SwipeActionType.DETAIL,
    val autofill: AutofillSettings = AutofillSettings(),
    val isSystemAutofillEnabled: Boolean = false,
)

sealed interface InteractionSettingsAction {
    data class SetSwipeEnabled(val enabled: Boolean) : InteractionSettingsAction
    data class SetSwipeLeftAction(val action: SwipeActionType) : InteractionSettingsAction
    data class SetSwipeRightAction(val action: SwipeActionType) : InteractionSettingsAction
    data class SetAutofillEnabled(val enabled: Boolean) : InteractionSettingsAction
    data class SetAutofillPresentation(
        val presentation: AutofillPresentation
    ) : InteractionSettingsAction

    data class SetCredentialManagerEnabled(val enabled: Boolean) : InteractionSettingsAction
    data class SetAutofillAuthenticationRequired(
        val required: Boolean
    ) : InteractionSettingsAction

    data class SetAutofillOtpEnabled(val enabled: Boolean) : InteractionSettingsAction
    data class SetAutofillSavePromptsEnabled(val enabled: Boolean) : InteractionSettingsAction
    data class SetUnmatchedAutofillSuggestionsEnabled(
        val enabled: Boolean
    ) : InteractionSettingsAction

    data class SetAutofillMaxSuggestions(val count: Int) : InteractionSettingsAction
}
