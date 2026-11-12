package com.aozijx.passly.feature.settings.presentation.ui.data

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.domain.settings.model.DiagnosticsLogRecordingMode
import com.aozijx.passly.feature.settings.presentation.main.data.StorageSettingsAction
import com.aozijx.passly.feature.settings.presentation.mapping.labelResource
import com.aozijx.passly.presentation.shared.components.group.SegmentedSettingsGroup
import com.aozijx.passly.presentation.shared.components.group.anchoredSelectionSettingsItem
import com.aozijx.passly.presentation.shared.components.group.model.SegmentedSettingsItem
import com.aozijx.passly.presentation.shared.components.group.model.SettingsItemValue
import com.aozijx.passly.presentation.shared.components.group.navigationSettingsGroupItem
import com.aozijx.passly.presentation.shared.components.group.settingsSegmentedColors

@Composable
internal fun LogSettingsSection(
    state: SettingsItemValue,
    logRecordingMode: RecoverableReadState<DiagnosticsLogRecordingMode>,
    onOpenDiagnostics: () -> Unit,
    onAction: (StorageSettingsAction) -> Unit,
    onClearLogs: () -> Unit,
) {
    var recordingModeMenuExpanded by rememberSaveable { mutableStateOf(false) }
    SettingsSectionTitle(text = stringResource(R.string.settings_log_management_title))
    SegmentedSettingsGroup(
        items =
            buildList {
                when (logRecordingMode) {
                    RecoverableReadState.Loading -> {
                        add(
                            SegmentedSettingsItem(key = "logs.recording.loading") { shapes ->
                                SegmentedListItem(
                                    shapes = shapes,
                                    colors = settingsSegmentedColors(),
                                    leadingContent = {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    },
                                    content = { Text(stringResource(R.string.settings_diagnostics_recording_title)) },
                                )
                            },
                        )
                    }

                    is RecoverableReadState.Failed -> {
                        add(
                            SegmentedSettingsItem(key = "logs.recording.failed") { shapes ->
                                SegmentedListItem(
                                    shapes = shapes,
                                    colors = settingsSegmentedColors(),
                                    content = { Text(stringResource(R.string.settings_read_failed)) },
                                    trailingContent = {
                                        TextButton(
                                            onClick = {
                                                onAction(StorageSettingsAction.RetryLogRecordingMode)
                                            },
                                        ) {
                                            Text(stringResource(R.string.retry))
                                        }
                                    },
                                )
                            },
                        )
                    }

                    is RecoverableReadState.Ready -> {
                        add(
                            anchoredSelectionSettingsItem(
                                key = "logs.recording_settings",
                                title = stringResource(R.string.settings_diagnostics_recording_title),
                                selected = logRecordingMode.value,
                                selectedLabel = stringResource(logRecordingMode.value.labelResource()),
                                options = DiagnosticsLogRecordingMode.entries.map { it to stringResource(it.labelResource()) },
                                expanded = recordingModeMenuExpanded,
                                onExpandedChange = { recordingModeMenuExpanded = it },
                                onSelect = { mode ->
                                    onAction(StorageSettingsAction.SetLogRecordingMode(mode))
                                },
                                icon = Icons.Default.Settings,
                            ),
                        )
                    }
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
                        value = state,
                        onClick = onClearLogs,
                    ),
                )
            },
    )
}
