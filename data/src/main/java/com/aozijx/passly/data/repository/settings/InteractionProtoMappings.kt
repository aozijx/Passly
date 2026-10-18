package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.data.preferences.proto.AutofillPreferences
import com.aozijx.passly.data.preferences.proto.AutofillPresentationPreference
import com.aozijx.passly.data.preferences.proto.InteractionPreferences
import com.aozijx.passly.data.preferences.proto.SwipeActionPreference
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.domain.settings.model.InteractionSettings
import com.aozijx.passly.domain.settings.model.SwipeActionType

internal fun SwipeActionPreference.toDomain(): SwipeActionType = when (this) {
    SwipeActionPreference.SWIPE_ACTION_DISABLED -> SwipeActionType.DISABLED
    SwipeActionPreference.SWIPE_ACTION_DELETE -> SwipeActionType.DELETE
    SwipeActionPreference.SWIPE_ACTION_COPY_PASSWORD -> SwipeActionType.COPY_PASSWORD
    SwipeActionPreference.SWIPE_ACTION_COPY_USERNAME -> SwipeActionType.COPY_USERNAME
    SwipeActionPreference.SWIPE_ACTION_UNSPECIFIED ->
        throw IllegalArgumentException("Unsupported swipe action: $this")
}

internal fun SwipeActionType.toProto(): SwipeActionPreference = when (this) {
    SwipeActionType.DISABLED -> SwipeActionPreference.SWIPE_ACTION_DISABLED
    SwipeActionType.DELETE -> SwipeActionPreference.SWIPE_ACTION_DELETE
    SwipeActionType.COPY_PASSWORD -> SwipeActionPreference.SWIPE_ACTION_COPY_PASSWORD
    SwipeActionType.COPY_USERNAME -> SwipeActionPreference.SWIPE_ACTION_COPY_USERNAME
}

internal fun AutofillPresentationPreference.toDomain(): AutofillPresentation = when (this) {
    AutofillPresentationPreference.AUTOFILL_PRESENTATION_SYSTEM_INLINE ->
        AutofillPresentation.SYSTEM_INLINE
    AutofillPresentationPreference.AUTOFILL_PRESENTATION_BOTTOM_SHEET ->
        AutofillPresentation.BOTTOM_SHEET
    AutofillPresentationPreference.AUTOFILL_PRESENTATION_UNSPECIFIED ->
        throw IllegalArgumentException("Unsupported autofill presentation: $this")
}

internal fun AutofillPresentation.toProto(): AutofillPresentationPreference = when (this) {
    AutofillPresentation.SYSTEM_INLINE ->
        AutofillPresentationPreference.AUTOFILL_PRESENTATION_SYSTEM_INLINE
    AutofillPresentation.BOTTOM_SHEET ->
        AutofillPresentationPreference.AUTOFILL_PRESENTATION_BOTTOM_SHEET
}

internal fun readInteraction(p: InteractionPreferences): InteractionSettings =
    InteractionSettings(
        isSwipeEnabled = p.swipeActionsEnabled,
        isPullToSearchEnabled = p.pullToSearchEnabled,
        swipeLeftAction = p.swipeLeftAction.toDomain(),
        swipeRightAction = p.swipeRightAction.toDomain(),
        autofill = readAutofill(p.autofill),
    )

internal fun readAutofill(p: AutofillPreferences): AutofillSettings =
    AutofillSettings(
        enabled = p.enabled,
        presentation = p.presentation.toDomain(),
        credentialManagerEnabled = p.credentialManagerEnabled,
        requireAuthentication = p.requireAuthentication,
        includeOtp = p.includeOtp,
        savePromptsEnabled = p.savePromptsEnabled,
        allowUnmatchedSuggestions = p.allowUnmatchedSuggestions,
        maxSuggestions = p.maxSuggestions.coerceIn(
            AutofillSettings.MIN_SUGGESTIONS,
            AutofillSettings.MAX_SUGGESTIONS,
        ),
    )
