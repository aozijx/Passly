package com.aozijx.passly.features.settings.general

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.features.settings.internal.SettingsContentActions
import com.aozijx.passly.features.settings.internal.SettingsContentState
import com.aozijx.passly.features.settings.shell.sectionSpacing

@Composable
internal fun GeneralDetail(
    state: SettingsContentState,
    actions: SettingsContentActions
) {
    Column(modifier = Modifier.sectionSpacing()) {
        Spacer(modifier = Modifier.height(8.dp))

        CacheSettingsSection(
            cacheSize = "12.5 MB",
            onClearCache = {}
        )

        Spacer(modifier = Modifier.height(24.dp))

        AboutSettingsSection(
            appVersion = "1.0.0",
            onAboutClick = {}
        )
    }
}