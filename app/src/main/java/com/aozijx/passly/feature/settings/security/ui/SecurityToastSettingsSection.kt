package com.aozijx.passly.feature.settings.security.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.ui.components.group.RoundedGroup
import com.aozijx.passly.ui.components.group.switchSettingsGroupItem
import com.aozijx.passly.ui.components.settings.SettingsSectionTitle

@Composable
internal fun SecurityToastSettingsSection(
    clipboardClearEnabled: Boolean,
    appCloseEnabled: Boolean,
    onClipboardClearEnabledChange: (Boolean) -> Unit,
    onAppCloseEnabledChange: (Boolean) -> Unit
) {
    SettingsSectionTitle(text = stringResource(R.string.settings_security_toast_messages))
    RoundedGroup(
        items = listOf(
            switchSettingsGroupItem(
                key = "security.toasts.clipboard_clear",
                title = stringResource(R.string.settings_clipboard_clear_toast),
                subtitle = stringResource(R.string.settings_clipboard_clear_toast_summary),
                checked = clipboardClearEnabled,
                onCheckedChange = onClipboardClearEnabledChange
            ),
            switchSettingsGroupItem(
                key = "security.toasts.app_close",
                title = stringResource(R.string.settings_app_close_toast),
                subtitle = stringResource(R.string.settings_app_close_toast_summary),
                checked = appCloseEnabled,
                onCheckedChange = onAppCloseEnabledChange
            )
        )
    )
}
