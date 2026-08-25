package com.aozijx.passly.presentation.feature.backup

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.backup.internal.model.BackupExportUiFormat
import com.aozijx.passly.feature.backup.internal.model.ImportMode
import com.aozijx.passly.presentation.ui.settings.backup.BackupEntryTypeUiModel
import com.aozijx.passly.presentation.ui.settings.backup.BackupExportFormatUiModel
import com.aozijx.passly.presentation.ui.settings.backup.BackupImportModeUiModel
import com.aozijx.passly.presentation.ui.settings.backup.BackupSheetUiState

internal fun BackupUiState.toSheetUiState(): BackupSheetUiState {
    val passwordChars = backupPassword.toCharArray()
    return try {
        BackupSheetUiState(
            password = passwordChars.concatToString(),
            importMode = BackupImportModeUiModel.valueOf(importMode.name),
            selectedExportFormat = BackupExportFormatUiModel.valueOf(selectedExportFormat.name),
            includeIcons = includeIcons,
            includeAttachments = includeAttachments,
            includeDeleted = includeDeleted,
            includedEntryTypes = includedEntryTypes.mapTo(linkedSetOf()) {
                BackupEntryTypeUiModel.valueOf(it.name)
            },
            canSubmitExport = canSubmitExport,
        )
    } finally {
        passwordChars.fill('\u0000')
    }
}

internal fun BackupExportFormatUiModel.toFeatureModel() = BackupExportUiFormat.valueOf(name)
internal fun BackupImportModeUiModel.toFeatureModel() = ImportMode.valueOf(name)
internal fun Set<BackupEntryTypeUiModel>.toFeatureModels(): Set<EntryType> =
    mapTo(linkedSetOf()) { EntryType.valueOf(it.name) }
