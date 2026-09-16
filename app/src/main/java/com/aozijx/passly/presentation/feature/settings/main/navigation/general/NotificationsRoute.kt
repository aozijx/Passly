package com.aozijx.passly.presentation.feature.settings.main.navigation.general

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.presentation.feature.settings.main.general.NotificationDetail
import com.aozijx.passly.presentation.ui.settings.main.component.SettingsGroup
import com.aozijx.passly.presentation.ui.settings.main.SettingsSecondaryPage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificationsRoute(
    onBack: (() -> Unit)?,
) {
    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.NOTIFICATIONS.titleRes),
        onBack = onBack
    ) {
        item { NotificationDetail() }
    }
}
