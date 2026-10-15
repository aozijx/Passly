package com.aozijx.passly.presentation.feature.settings.ui.general

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.presentation.shared.components.group.SegmentedSettingsGroup
import com.aozijx.passly.presentation.shared.components.group.navigationSettingsGroupItem

@Composable
fun LogSettingsSection(onOpenDiagnostics: () -> Unit) {
    SettingsSectionTitle(text = stringResource(R.string.settings_log_management_title))
    SegmentedSettingsGroup(
        items = listOf(
            navigationSettingsGroupItem(
                key = "logs.view",
                icon = Icons.Default.BugReport,
                title = stringResource(R.string.settings_log_view_title),
                subtitle = stringResource(R.string.settings_log_view_subtitle),
                onClick = onOpenDiagnostics,
            ),
        ),
    )
}
