package com.aozijx.passly.presentation.feature.settings.autofill.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.presentation.feature.settings.autofill.AutofillSettingsAction
import com.aozijx.passly.presentation.feature.settings.autofill.AutofillSettingsUiState

@Composable
internal fun AutofillDetail(
    state: AutofillSettingsUiState,
    onOpenAutofillSettings: () -> Unit,
    onAction: (AutofillSettingsAction) -> Unit,
) {
    SettingsSection {
        Spacer(modifier = Modifier.height(8.dp))

        AutofillSettingsSection(
            settings = state.autofill,
            isSystemServiceEnabled = state.isSystemAutofillEnabled,
            onOpenAutofillSettings = onOpenAutofillSettings,
            onAction = onAction,
        )
    }
}
