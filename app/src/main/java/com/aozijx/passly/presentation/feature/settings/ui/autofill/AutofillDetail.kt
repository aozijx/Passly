package com.aozijx.passly.presentation.feature.settings.ui.autofill

import androidx.compose.runtime.Composable
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.presentation.feature.settings.ui.autofill.model.AutofillSettingsEvent
import com.aozijx.passly.presentation.feature.settings.ui.autofill.model.AutofillSettingsUiModel

@Composable
internal fun AutofillDetail(
    state: AutofillSettingsUiModel,
    onEvent: (AutofillSettingsEvent) -> Unit,
) {
    SettingsSection {
        AutofillSettingsSection(
            settings = state,
            onEvent = onEvent,
        )
    }
}
