package com.aozijx.passly.presentation.feature.settings.security

import com.aozijx.passly.feature.settings.security.AppPasswordChangeRequest

sealed interface AppPasswordSettingsAction {
    data object RequestAppPasswordEntry : AppPasswordSettingsAction
    data class SubmitChange(
        val request: AppPasswordChangeRequest,
    ) : AppPasswordSettingsAction
}
