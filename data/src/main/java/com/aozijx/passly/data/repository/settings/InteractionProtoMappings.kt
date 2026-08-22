package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.data.local.datastore.settings.AutofillPreferences
import com.aozijx.passly.data.local.datastore.settings.InteractionPreferences
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.domain.settings.model.InteractionSettings
import com.aozijx.passly.domain.settings.model.SwipeActionType

// -- SwipeActionType --
internal fun String.toSwipeActionDomain(): SwipeActionType = when (this) {
    "delete" -> SwipeActionType.DELETE
    "detail" -> SwipeActionType.DETAIL
    "copy_username" -> SwipeActionType.COPY_USERNAME
    "copy_password" -> SwipeActionType.COPY_PASSWORD
    else -> SwipeActionType.COPY_PASSWORD
}

internal fun SwipeActionType.toSwipeActionString(): String = when (this) {
    SwipeActionType.DELETE -> "delete"
    SwipeActionType.DETAIL -> "detail"
    SwipeActionType.COPY_PASSWORD -> "copy_password"
    SwipeActionType.COPY_USERNAME -> "copy_username"
}

// -- Autofill presentation --
internal fun String.toAutofillPresentationDomain(): AutofillPresentation = when (this) {
    "bottom_sheet" -> AutofillPresentation.BOTTOM_SHEET
    else -> AutofillPresentation.SYSTEM_INLINE
}

internal fun AutofillPresentation.toStorageKey(): String = when (this) {
    AutofillPresentation.SYSTEM_INLINE -> "system_inline"
    AutofillPresentation.BOTTOM_SHEET -> "bottom_sheet"
}

internal fun readInteraction(p: InteractionPreferences): InteractionSettings =
    InteractionSettings(
        isSwipeEnabled = p.swipeActionsEnabled,
        swipeLeftAction = p.swipeLeftAction.toSwipeActionDomain(),
        swipeRightAction = p.swipeRightAction.toSwipeActionDomain(),
        autofill = readAutofill(p.autofill),
        isAutoDownloadIcons = p.autoDownloadIcons,
        faviconDownloadWhitelist = p.faviconAllowedDomainsList.toSet()
    )

internal fun readAutofill(p: AutofillPreferences): AutofillSettings =
    AutofillSettings(
        enabled = p.enabled,
        presentation = p.presentation.toAutofillPresentationDomain(),
        credentialManagerEnabled = p.credentialManagerEnabled,
        requireAuthentication = p.requireAuthentication,
        includeOtp = p.includeOtp,
        savePromptsEnabled = p.savePromptsEnabled,
        allowUnmatchedSuggestions = p.allowUnmatchedSuggestions,
        maxSuggestions = p.maxSuggestions.coerceIn(
            AutofillSettings.MIN_SUGGESTIONS,
            AutofillSettings.MAX_SUGGESTIONS
        )
    )
