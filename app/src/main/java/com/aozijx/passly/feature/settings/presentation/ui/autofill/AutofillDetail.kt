package com.aozijx.passly.feature.settings.presentation.ui.autofill

import androidx.compose.runtime.Composable
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.feature.settings.presentation.autofill.AutofillSettingsAction
import com.aozijx.passly.feature.settings.presentation.ui.autofill.model.AutofillSettingsUiModel

@Composable
internal fun AutofillDetail(
    state: AutofillSettingsUiModel,
    onAction: (AutofillSettingsAction) -> Unit,
) {
    SettingsSection {
        AutofillSettingsSection(
            settings = state,
            onAction = onAction,
        )
    }
}
