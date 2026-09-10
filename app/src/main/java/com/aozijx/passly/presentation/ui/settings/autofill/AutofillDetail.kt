package com.aozijx.passly.presentation.ui.settings.autofill

import androidx.compose.runtime.Composable
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.presentation.ui.settings.autofill.model.AutofillSettingsEventHandler
import com.aozijx.passly.presentation.ui.settings.autofill.model.AutofillSettingsUiModel

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
