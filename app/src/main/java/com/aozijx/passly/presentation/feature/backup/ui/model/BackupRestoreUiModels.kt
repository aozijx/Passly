package com.aozijx.passly.presentation.feature.backup.ui.model

import com.aozijx.passly.feature.backup.internal.model.BackupExportFormat
import com.aozijx.passly.feature.backup.internal.model.ImportMode
import com.aozijx.passly.presentation.shared.entry.EntryTypeUiModel

internal enum class BackupSheet {
    FORMAT_PICKER,
    EXPORT_OPTIONS,
    IMPORT_OPTIONS,
}

internal data class BackupRestoreSheetUiState(
    val activeSheet: BackupSheet?,
    val configuredDirectoryLabel: String?,
    val password: String,
    val importMode: ImportMode,
    val selectedExportFormat: BackupExportFormat,
    val includeIcons: Boolean,
    val includeAttachments: Boolean,
    val includeDeleted: Boolean,
    val includedEntryTypes: Set<EntryTypeUiModel>,
    val canSubmitExport: Boolean,
)

internal sealed interface BackupSheetEvent {
    data object Dismissed : BackupSheetEvent
    data class FormatSelected(val format: BackupExportFormat) : BackupSheetEvent
    data class PasswordChanged(val password: String) : BackupSheetEvent
    data class IncludeIconsChanged(val include: Boolean) : BackupSheetEvent
    data class IncludeAttachmentsChanged(val include: Boolean) : BackupSheetEvent
    data class IncludeDeletedChanged(val include: Boolean) : BackupSheetEvent
    data class IncludedEntryTypesChanged(
        val types: Set<EntryTypeUiModel>,
    ) : BackupSheetEvent
    data class ImportModeChanged(val mode: ImportMode) : BackupSheetEvent
    data object ExportRequested : BackupSheetEvent
    data object ImportRequested : BackupSheetEvent
}
