package com.aozijx.passly.presentation.feature.settings.ui.autofill

import androidx.compose.runtime.Composable
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.presentation.feature.settings.ui.autofill.model.AutofillSettingsEventHandler
import com.aozijx.passly.presentation.feature.settings.ui.autofill.model.AutofillSettingsUiModel

@Composable
internal fun AutofillDetail(
    state: AutofillSettingsUiModel,
    eventHandler: AutofillSettingsEventHandler,
) {
    SettingsSection {
        AutofillSettingsSection(
            settings = state,
            eventHandler = eventHandler,
        )
    }
}
