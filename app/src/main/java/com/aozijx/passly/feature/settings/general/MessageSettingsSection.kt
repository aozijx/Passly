package com.aozijx.passly.feature.settings.general

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.feature.settings.components.switchSettingsItem
import com.aozijx.passly.feature.settings.shell.SettingsGroupTitle
import com.aozijx.passly.feature.settings.shell.SettingsRoundedGroup

@Composable
internal fun MessageSettingsSection(
    state: MessageSettingsUiState,
    onShowGeneralChange: (Boolean) -> Unit,
    onShowIconDownloadsChange: (Boolean) -> Unit,
    onShowClipboardClearsChange: (Boolean) -> Unit,
    onShowAppCloseChange: (Boolean) -> Unit
) {
    SettingsGroupTitle(text = stringResource(R.string.settings_messages))
    SettingsRoundedGroup {
        switchSettingsItem(
            title = stringResource(R.string.settings_messages_general),
            subtitle = stringResource(R.string.settings_messages_general_summary),
            checked = state.showGeneral,
            onCheckedChange = onShowGeneralChange
        )
        switchSettingsItem(
            visible = state.showGeneral,
            title = stringResource(R.string.settings_messages_icon_download),
            subtitle = stringResource(R.string.settings_messages_icon_download_summary),
            checked = state.showIconDownloads,
            onCheckedChange = onShowIconDownloadsChange
        )
        switchSettingsItem(
            visible = state.showGeneral,
            title = stringResource(R.string.settings_messages_clipboard),
            subtitle = stringResource(R.string.settings_messages_clipboard_summary),
            checked = state.showClipboardClears,
            onCheckedChange = onShowClipboardClearsChange
        )
        switchSettingsItem(
            visible = state.showGeneral,
            title = stringResource(R.string.settings_messages_app_close),
            subtitle = stringResource(R.string.settings_messages_app_close_summary),
            checked = state.showAppClose,
            onCheckedChange = onShowAppCloseChange
        )
    }
}
