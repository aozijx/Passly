package com.aozijx.passly.feature.settings.presentation.autofill

import com.aozijx.passly.R
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.presentation.shared.error.UiErrorMessage

sealed interface AutofillPreferencesState {
    sealed interface Unavailable : AutofillPreferencesState {
        val statusTextRes: Int
        val canRetry: Boolean
    }

    data object Loading : Unavailable {
        override val statusTextRes = R.string.settings_autofill_preferences_loading
        override val canRetry = false
    }

    data class Ready(
        val settings: AutofillSettings,
    ) : AutofillPreferencesState

    data object Failed : Unavailable {
        override val statusTextRes = R.string.settings_autofill_preferences_load_failed
        override val canRetry = true
    }
}

sealed interface AutofillSystemServiceStatus {
    val statusTextRes: Int
    val canRetry: Boolean
        get() = false
    val lastKnownEnabled: Boolean?

    data object Checking : AutofillSystemServiceStatus {
        override val statusTextRes = R.string.settings_autofill_system_status_loading
        override val lastKnownEnabled: Boolean? = null
    }

    data class Refreshing(
        override val lastKnownEnabled: Boolean,
    ) : AutofillSystemServiceStatus {
        override val statusTextRes = lastKnownEnabled.toAutofillStatusTextRes()
    }

    data class Available(
        val isEnabled: Boolean,
    ) : AutofillSystemServiceStatus {
        override val statusTextRes = isEnabled.toAutofillStatusTextRes()
        override val lastKnownEnabled: Boolean = isEnabled
    }

    data class Unavailable(
        override val lastKnownEnabled: Boolean?,
    ) : AutofillSystemServiceStatus {
        override val statusTextRes = R.string.settings_autofill_system_status_unavailable
        override val canRetry = true
    }
}

private fun Boolean.toAutofillStatusTextRes(): Int =
    if (this) R.string.settings_autofill_system_enabled else R.string.settings_autofill_system_disabled

data class AutofillSettingsUiState(
    val preferences: AutofillPreferencesState = AutofillPreferencesState.Loading,
    val systemServiceStatus: AutofillSystemServiceStatus = AutofillSystemServiceStatus.Checking,
)

sealed interface AutofillSettingsAction {
    data class SetEnabled(
        val enabled: Boolean,
    ) : AutofillSettingsAction

    data class SetPresentation(
        val presentation: AutofillPresentation,
    ) : AutofillSettingsAction

    data class SetCredentialManagerEnabled(
        val enabled: Boolean,
    ) : AutofillSettingsAction

    data class SetAuthenticationRequired(
        val required: Boolean,
    ) : AutofillSettingsAction

    data class SetStrictDomainMatching(
        val enabled: Boolean,
    ) : AutofillSettingsAction

    data object RetryPreferences : AutofillSettingsAction

    data object RetrySystemService : AutofillSettingsAction

    data object OpenSystemAutofillSettings : AutofillSettingsAction

    data object OpenAccessibilitySettings : AutofillSettingsAction
}

sealed interface AutofillSettingsEffect {
    data class SaveFailed(
        val message: UiErrorMessage,
    ) : AutofillSettingsEffect

    data object OpenSystemAutofillSettings : AutofillSettingsEffect

    data object OpenAccessibilitySettings : AutofillSettingsEffect
}
