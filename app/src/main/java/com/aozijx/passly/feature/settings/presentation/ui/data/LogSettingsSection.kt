package com.aozijx.passly.feature.settings.presentation.ui.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.domain.settings.model.DiagnosticsLogRecordingMode
import com.aozijx.passly.feature.settings.presentation.main.data.StorageSizeUiState
import com.aozijx.passly.feature.settings.presentation.mapping.labelResource
import com.aozijx.passly.presentation.shared.components.group.SegmentedSettingsGroup
import com.aozijx.passly.presentation.shared.components.group.anchoredSelectionSettingsItem
import com.aozijx.passly.presentation.shared.components.group.navigationSettingsGroupItem

@Composable
internal fun LogSettingsSection(
    state: StorageSizeUiState,
    logRecordingMode: DiagnosticsLogRecordingMode?,
    onOpenDiagnostics: () -> Unit,
    onLogRecordingModeChange: (DiagnosticsLogRecordingMode) -> Unit,
    onClearLogs: () -> Unit,
) {
    var recordingModeMenuExpanded by rememberSaveable { mutableStateOf(false) }
    SettingsSectionTitle(text = stringResource(R.string.settings_log_management_title))
    SegmentedSettingsGroup(
        items =
            buildList {
                if (logRecordingMode != null) {
                    add(
                        anchoredSelectionSettingsItem(
                            key = "logs.recording_settings",
                            title = stringResource(R.string.settings_diagnostics_recording_title),
                            selected = logRecordingMode,
                            selectedLabel = stringResource(logRecordingMode.labelResource()),
                            options = DiagnosticsLogRecordingMode.entries.map { it to stringResource(it.labelResource()) },
                            expanded = recordingModeMenuExpanded,
                            onExpandedChange = { recordingModeMenuExpanded = it },
                            onSelect = onLogRecordingModeChange,
                            icon = Icons.Default.Settings,
                        ),
                    )
                }
                add(
                    navigationSettingsGroupItem(
                        key = "logs.view",
                        icon = Icons.Default.BugReport,
                        title = stringResource(R.string.settings_diagnostics_title),
                        subtitle = stringResource(R.string.settings_log_view_subtitle),
                        onClick = onOpenDiagnostics,
                    ),
                )
                add(
                    navigationSettingsGroupItem(
                        key = "logs.clear",
                        icon = Icons.Default.DeleteSweep,
                        title = stringResource(R.string.settings_log_clear_action),
                        value = (state as? StorageSizeUiState.Available)?.formattedSize,
                        isLoading = state is StorageSizeUiState.Loading,
                        onClick = onClearLogs,
                    ),
                )
            },
    )
}
