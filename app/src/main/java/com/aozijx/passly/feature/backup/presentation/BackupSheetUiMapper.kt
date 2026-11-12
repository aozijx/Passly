package com.aozijx.passly.feature.backup.presentation

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.backup.contract.BackupExportFormat
import com.aozijx.passly.feature.backup.presentation.ui.model.BackupRestoreSheetUiState

internal fun BackupUiState.toSheetUiState(configuredDirectoryLabel: String?): BackupRestoreSheetUiState =
    when (val current = workflow) {
        BackupWorkflow.Idle,
        is BackupWorkflow.AwaitingExportTarget,
        is BackupWorkflow.Executing,
        is BackupWorkflow.Failed,
        -> BackupRestoreSheetUiState.Hidden

        BackupWorkflow.FormatPicker -> BackupRestoreSheetUiState.FormatPicker

        is BackupWorkflow.ConfigureExport -> current.options.toUiState(configuredDirectoryLabel)

        is BackupWorkflow.ConfigureImport -> current.options.toUiState()
    }

private fun BackupExportOptions.toUiState(configuredDirectoryLabel: String?) =
    BackupRestoreSheetUiState.ExportOptions(
        configuredDirectoryLabel = configuredDirectoryLabel,
        password = password.copyToString(),
        titleResource = format.optionsTitleResource(),
        securityNoticeResource = format.securityNoticeResource(),
        securityNoticeIsWarning = format.securityNoticeIsWarning(),
        requiresPassword = format.requiresPassword,
        supportsResources = format.supportsResources,
        includeIcons = includeIcons,
        includeAttachments = includeAttachments,
        includeDeleted = includeDeleted,
        includedEntryTypes = includedEntryTypes,
        allEntryTypesSelected = allEntryTypesSelected,
        needsEntryTypeSelection = needsEntryTypeSelection,
        canSubmit = canSubmit,
    )

@StringRes
private fun BackupExportFormat.optionsTitleResource(): Int =
    when (this) {
        BackupExportFormat.ENCRYPTED -> R.string.settings_backup_encrypted_options_title
        BackupExportFormat.JSON -> R.string.settings_backup_json_options_title
        BackupExportFormat.TEXT -> R.string.settings_backup_text_options_title
    }

@StringRes
private fun BackupExportFormat.securityNoticeResource(): Int =
    when (this) {
        BackupExportFormat.ENCRYPTED -> R.string.settings_backup_encrypted_security_notice
        BackupExportFormat.JSON -> R.string.settings_backup_json_security_notice
        BackupExportFormat.TEXT -> R.string.settings_backup_text_security_notice
    }

private fun BackupExportFormat.securityNoticeIsWarning(): Boolean =
    when (this) {
        BackupExportFormat.ENCRYPTED -> false

        BackupExportFormat.JSON,
        BackupExportFormat.TEXT,
        -> true
    }

private fun BackupImportOptions.toUiState() =
    BackupRestoreSheetUiState.ImportOptions(
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
