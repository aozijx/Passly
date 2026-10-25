package com.aozijx.passly.feature.settings.presentation.security

import com.aozijx.passly.feature.settings.security.AppPasswordChangeRequest

sealed interface AppPasswordSettingsAction {
    data object RequestAppPasswordEntry : AppPasswordSettingsAction
    data class SubmitChange(
        val request: AppPasswordChangeRequest,
    ) : AppPasswordSettingsAction
}
