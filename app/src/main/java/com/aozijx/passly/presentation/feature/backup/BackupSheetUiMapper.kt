package com.aozijx.passly.presentation.feature.backup

import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.presentation.feature.backup.ui.model.BackupRestoreSheetUiState
import com.aozijx.passly.presentation.mapping.entry.toUiModel

internal fun BackupUiState.toSheetUiState(
    configuredDirectoryLabel: String?,
): BackupRestoreSheetUiState = when (val current = workflow) {
    BackupWorkflow.Idle,
    is BackupWorkflow.AwaitingExportTarget,
    is BackupWorkflow.Executing,
    is BackupWorkflow.Failed -> BackupRestoreSheetUiState.Hidden
    BackupWorkflow.FormatPicker -> BackupRestoreSheetUiState.FormatPicker
    is BackupWorkflow.ConfigureExport -> current.options.toUiState(configuredDirectoryLabel)
    is BackupWorkflow.ConfigureImport -> current.options.toUiState()
}

private fun BackupExportOptions.toUiState(
    configuredDirectoryLabel: String?,
) = BackupRestoreSheetUiState.ExportOptions(
    configuredDirectoryLabel = configuredDirectoryLabel,
    password = password.copyToString(),
    format = format,
    includeIcons = includeIcons,
    includeAttachments = includeAttachments,
    includeDeleted = includeDeleted,
    includedEntryTypes = includedEntryTypes.mapTo(linkedSetOf()) { it.toUiModel() },
    canSubmit = canSubmit,
)

private fun BackupImportOptions.toUiState() = BackupRestoreSheetUiState.ImportOptions(
    password = password.copyToString(),
    mode = mode,
    strategy = strategy,
)

private fun SensitiveValue.copyToString(): String {
    val chars = toCharArray()
    return try {
        chars.concatToString()
    } finally {
        chars.fill('\u0000')
    }
}
