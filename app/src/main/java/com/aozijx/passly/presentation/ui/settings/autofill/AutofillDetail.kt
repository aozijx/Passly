package com.aozijx.passly.presentation.ui.settings.autofill

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.presentation.ui.settings.autofill.model.AutofillPresentationUiModel
import com.aozijx.passly.presentation.ui.settings.autofill.model.AutofillSettingsUiModel

@Composable
internal fun AutofillDetail(
    state: AutofillSettingsUiModel,
    onOpenAutofillSettings: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onPresentationChange: (AutofillPresentationUiModel) -> Unit,
    onCredentialManagerEnabledChange: (Boolean) -> Unit,
    onAuthenticationRequiredChange: (Boolean) -> Unit,
    onOtpEnabledChange: (Boolean) -> Unit,
    onSavePromptsEnabledChange: (Boolean) -> Unit,
    onUnmatchedSuggestionsEnabledChange: (Boolean) -> Unit,
    onMaxSuggestionsChange: (Int) -> Unit,
) {
    SettingsSection {
        Spacer(modifier = Modifier.height(8.dp))

        AutofillSettingsSection(
            settings = state,
            onOpenAutofillSettings = onOpenAutofillSettings,
            onEnabledChange = onEnabledChange,
            onPresentationChange = onPresentationChange,
            onCredentialManagerEnabledChange = onCredentialManagerEnabledChange,
            onAuthenticationRequiredChange = onAuthenticationRequiredChange,
            onOtpEnabledChange = onOtpEnabledChange,
            onSavePromptsEnabledChange = onSavePromptsEnabledChange,
            onUnmatchedSuggestionsEnabledChange = onUnmatchedSuggestionsEnabledChange,
            onMaxSuggestionsChange = onMaxSuggestionsChange,
        )
    }
}
