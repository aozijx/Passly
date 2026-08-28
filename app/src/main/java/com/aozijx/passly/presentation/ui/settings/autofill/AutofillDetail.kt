package com.aozijx.passly.presentation.ui.settings.autofill

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.presentation.ui.settings.autofill.model.AutofillSettingsEventHandler
import com.aozijx.passly.presentation.ui.settings.autofill.model.AutofillSettingsUiModel

@Composable
internal fun AutofillDetail(
    state: AutofillSettingsUiModel,
    eventHandler: AutofillSettingsEventHandler,
) {
    SettingsSection {
        Spacer(modifier = Modifier.height(8.dp))

        AutofillSettingsSection(
            settings = state,
            eventHandler = eventHandler,
        )
    }
}
