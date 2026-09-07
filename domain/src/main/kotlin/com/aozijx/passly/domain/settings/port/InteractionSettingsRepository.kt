package com.aozijx.passly.domain.settings.port

import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.InteractionSettings
import com.aozijx.passly.domain.settings.model.SwipeActionType
import kotlinx.coroutines.flow.Flow

interface InteractionSettingsSource {
    val interaction: Flow<InteractionSettings>
}

interface InteractionSettingsRepository : InteractionSettingsSource {
    suspend fun setSwipeEnabled(enabled: Boolean)
    suspend fun setSwipeLeftAction(action: SwipeActionType)
    suspend fun setSwipeRightAction(action: SwipeActionType)
    suspend fun setAutofillEnabled(enabled: Boolean)
    suspend fun setAutofillPresentation(presentation: AutofillPresentation)
    suspend fun setCredentialManagerEnabled(enabled: Boolean)
    suspend fun setAutofillAuthenticationRequired(required: Boolean)
    suspend fun setAutofillOtpEnabled(enabled: Boolean)
    suspend fun setAutofillSavePromptsEnabled(enabled: Boolean)
    suspend fun setUnmatchedAutofillSuggestionsEnabled(enabled: Boolean)
    suspend fun setAutofillMaxSuggestions(count: Int)
}
