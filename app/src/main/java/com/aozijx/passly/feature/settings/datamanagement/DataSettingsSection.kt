package com.aozijx.passly.feature.settings.datamanagement

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.feature.settings.components.switchSettingsItem
import com.aozijx.passly.feature.settings.shell.SettingsGroupTitle
import com.aozijx.passly.feature.settings.shell.SettingsRoundedGroup

@Composable
fun DataSettingsSection(
    isAutoDownloadIcons: Boolean,
    onAutoDownloadIconsChange: (Boolean) -> Unit
) {
    SettingsGroupTitle(text = stringResource(R.string.settings_data_download))
    SettingsRoundedGroup {
        switchSettingsItem(
            icon = Icons.Default.CloudDownload,
            title = stringResource(R.string.settings_download_icons),
            subtitle = stringResource(R.string.settings_download_icons_subtitle),
            checked = isAutoDownloadIcons,
            onCheckedChange = onAutoDownloadIconsChange
        )
    }
}
