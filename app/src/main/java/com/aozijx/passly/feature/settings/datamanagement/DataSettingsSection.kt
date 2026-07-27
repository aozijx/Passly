package com.aozijx.passly.feature.settings.datamanagement

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.group.RoundedGroup
import com.aozijx.passly.core.ui.components.group.switchSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle

@Composable
fun DataSettingsSection(
    isAutoDownloadIcons: Boolean,
    onAutoDownloadIconsChange: (Boolean) -> Unit
) {
    SettingsSectionTitle(text = stringResource(R.string.settings_data_download))
    RoundedGroup(
        items = listOf(
            switchSettingsGroupItem(
                key = "data.auto_download_icons",
                icon = Icons.Default.CloudDownload,
                title = stringResource(R.string.settings_download_icons),
                subtitle = stringResource(R.string.settings_download_icons_subtitle),
                checked = isAutoDownloadIcons,
                onCheckedChange = onAutoDownloadIconsChange
            )
        )
    )
}
