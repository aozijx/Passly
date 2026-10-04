package com.aozijx.passly.presentation.feature.backup

import com.aozijx.passly.presentation.feature.backup.ui.model.BackupRestoreSheetUiState
import com.aozijx.passly.presentation.mapping.entry.toUiModel

internal fun BackupUiState.toSheetUiState(
    configuredDirectoryLabel: String?,
): BackupRestoreSheetUiState {
    val passwordChars = backupPassword.toCharArray()
    return try {
        BackupRestoreSheetUiState(
            activeSheet = optionsStage,
            configuredDirectoryLabel = configuredDirectoryLabel,
            password = passwordChars.concatToString(),
            importMode = importMode,
            importStrategy = importStrategy,
            selectedExportFormat = selectedExportFormat,
            includeIcons = includeIcons,
            includeAttachments = includeAttachments,
            includeDeleted = includeDeleted,
            includedEntryTypes = includedEntryTypes.mapTo(linkedSetOf()) {
                it.toUiModel()
            },
            canSubmitExport = canSubmitExport,
        )
    } finally {
        passwordChars.fill('\u0000')
    }
}
