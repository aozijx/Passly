package com.aozijx.passly.feature.settings.presentation.ui.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.feature.settings.presentation.main.data.StorageSizeUiState
import com.aozijx.passly.presentation.shared.components.group.SegmentedSettingsGroup
import com.aozijx.passly.presentation.shared.components.group.navigationSettingsGroupItem

@Composable
internal fun LogSettingsSection(
    state: StorageSizeUiState,
    onOpenDiagnostics: () -> Unit,
    onClearLogs: () -> Unit,
) {
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
            navigationSettingsGroupItem(
                key = "logs.clear",
                icon = Icons.Default.DeleteSweep,
                title = stringResource(R.string.settings_log_clear_action),
                value = (state as? StorageSizeUiState.Available)?.formattedSize,
                isLoading = state is StorageSizeUiState.Loading,
                onClick = onClearLogs,
            ),
        ),
    )
}
