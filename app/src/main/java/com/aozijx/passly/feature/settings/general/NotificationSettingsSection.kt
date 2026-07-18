package com.aozijx.passly.feature.settings.general

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.ui.components.group.RoundedGroup
import com.aozijx.passly.ui.components.group.switchSettingsGroupItem
import com.aozijx.passly.ui.components.settings.SettingsSectionTitle

@Composable
internal fun NotificationSettingsSection(
    state: NotificationSettingsUiState,
    onStatusBarEnabledChange: (Boolean) -> Unit,
    onIconDownloadsEnabledChange: (Boolean) -> Unit
) {
    SettingsSectionTitle(text = stringResource(R.string.settings_status_bar_notifications))
    RoundedGroup(
        items = listOf(
            switchSettingsGroupItem(
                key = "notifications.status_bar",
                title = stringResource(R.string.settings_status_bar_notifications),
                subtitle = stringResource(R.string.settings_status_bar_notifications_summary),
                checked = state.statusBarEnabled,
                onCheckedChange = onStatusBarEnabledChange
            ),
            switchSettingsGroupItem(
                key = "notifications.icon_download",
                visible = state.statusBarEnabled,
                title = stringResource(R.string.settings_icon_download_notifications),
                subtitle = stringResource(R.string.settings_icon_download_notifications_summary),
                checked = state.iconDownloadsEnabled,
                onCheckedChange = onIconDownloadsEnabledChange
            )
        )
    )
}
