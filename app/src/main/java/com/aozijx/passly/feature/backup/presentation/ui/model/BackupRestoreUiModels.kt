package com.aozijx.passly.feature.backup.presentation.ui.model

import com.aozijx.passly.feature.backup.internal.model.BackupExportFormat
import com.aozijx.passly.feature.backup.internal.model.BackupImportStrategy
import com.aozijx.passly.feature.backup.internal.model.ImportMode
import com.aozijx.passly.domain.entry.model.EntryType

internal sealed interface BackupRestoreSheetUiState {
    data object Hidden : BackupRestoreSheetUiState
    data object FormatPicker : BackupRestoreSheetUiState
    data class ExportOptions(
        val configuredDirectoryLabel: String?,
        val password: String,
        val format: BackupExportFormat,
        val includeIcons: Boolean,
        val includeAttachments: Boolean,
        val includeDeleted: Boolean,
        val includedEntryTypes: Set<EntryType>,
        val canSubmit: Boolean,
    ) : BackupRestoreSheetUiState
    data class ImportOptions(
        val password: String,
        val mode: ImportMode,
        val strategy: BackupImportStrategy,
    ) : BackupRestoreSheetUiState
}
