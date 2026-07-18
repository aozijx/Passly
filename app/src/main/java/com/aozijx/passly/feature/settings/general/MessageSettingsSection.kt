package com.aozijx.passly.feature.settings.general

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.ui.components.group.RoundedGroup
import com.aozijx.passly.ui.components.group.switchSettingsGroupItem
import com.aozijx.passly.ui.components.settings.SettingsSectionTitle

@Composable
internal fun MessageSettingsSection(
    state: MessageSettingsUiState,
    onShowGeneralChange: (Boolean) -> Unit,
    onShowIconDownloadsChange: (Boolean) -> Unit,
    onShowClipboardClearsChange: (Boolean) -> Unit,
    onShowAppCloseChange: (Boolean) -> Unit
) {
    SettingsSectionTitle(text = stringResource(R.string.settings_messages))
    RoundedGroup(
        items = listOf(
            switchSettingsGroupItem(
                key = "messages.general",
                title = stringResource(R.string.settings_messages_general),
                subtitle = stringResource(R.string.settings_messages_general_summary),
                checked = state.showGeneral,
                onCheckedChange = onShowGeneralChange
            ),
            switchSettingsGroupItem(
                key = "messages.icon_download",
                visible = state.showGeneral,
                title = stringResource(R.string.settings_messages_icon_download),
                subtitle = stringResource(R.string.settings_messages_icon_download_summary),
                checked = state.showIconDownloads,
                onCheckedChange = onShowIconDownloadsChange
            ),
            switchSettingsGroupItem(
                key = "messages.clipboard",
                visible = state.showGeneral,
                title = stringResource(R.string.settings_messages_clipboard),
                subtitle = stringResource(R.string.settings_messages_clipboard_summary),
                checked = state.showClipboardClears,
                onCheckedChange = onShowClipboardClearsChange
            ),
            switchSettingsGroupItem(
                key = "messages.app_close",
                visible = state.showGeneral,
                title = stringResource(R.string.settings_messages_app_close),
                subtitle = stringResource(R.string.settings_messages_app_close_summary),
                checked = state.showAppClose,
                onCheckedChange = onShowAppCloseChange
            )
        )
    )
}
