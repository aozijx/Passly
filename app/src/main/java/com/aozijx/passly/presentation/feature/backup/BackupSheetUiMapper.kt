package com.aozijx.passly.presentation.feature.backup

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.backup.internal.model.BackupExportFormat
import com.aozijx.passly.feature.backup.internal.model.ImportMode
import com.aozijx.passly.presentation.ui.settings.backup.model.BackupExportFormatUiModel
import com.aozijx.passly.presentation.ui.settings.backup.model.BackupImportModeUiModel
import com.aozijx.passly.presentation.ui.settings.backup.model.BackupRestoreSheetUiState
import com.aozijx.passly.presentation.ui.settings.backup.model.BackupSheet
import com.aozijx.passly.presentation.ui.shared.entry.EntryTypeUiModel

internal fun BackupUiState.toSheetUiState(
    activeSheet: BackupSheet?,
    configuredDirectoryLabel: String?,
): BackupRestoreSheetUiState {
    val passwordChars = backupPassword.toCharArray()
    return try {
        BackupRestoreSheetUiState(
            activeSheet = activeSheet,
            configuredDirectoryLabel = configuredDirectoryLabel,
            password = passwordChars.concatToString(),
            importMode = BackupImportModeUiModel.valueOf(importMode.name),
            selectedExportFormat = BackupExportFormatUiModel.valueOf(selectedExportFormat.name),
            includeIcons = includeIcons,
            includeAttachments = includeAttachments,
            includeDeleted = includeDeleted,
            includedEntryTypes = includedEntryTypes.mapTo(linkedSetOf()) {
                EntryTypeUiModel.valueOf(it.name)
            },
            canSubmitExport = canSubmitExport,
        )
    } finally {
        passwordChars.fill('\u0000')
    }
}

internal fun BackupExportFormatUiModel.toFeatureModel() = BackupExportFormat.valueOf(name)
internal fun BackupImportModeUiModel.toFeatureModel() = ImportMode.valueOf(name)
internal fun Set<EntryTypeUiModel>.toFeatureModels(): Set<EntryType> =
    mapTo(linkedSetOf()) { EntryType.valueOf(it.name) }
