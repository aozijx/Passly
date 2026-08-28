package com.aozijx.passly.presentation.ui.settings.backup.model

import com.aozijx.passly.presentation.ui.shared.entry.EntryTypeUiModel

internal enum class BackupExportFormatUiModel(
    val requiresPassword: Boolean,
    val supportsResources: Boolean,
) {
    ENCRYPTED(requiresPassword = true, supportsResources = true),
    JSON(requiresPassword = false, supportsResources = false),
    TEXT(requiresPassword = false, supportsResources = false),
}

internal enum class BackupImportModeUiModel { APPEND, OVERWRITE }

internal enum class BackupSheet {
    FORMAT_PICKER,
    EXPORT_OPTIONS,
    IMPORT_OPTIONS,
}

internal data class BackupRestoreSheetUiState(
    val activeSheet: BackupSheet?,
    val configuredDirectoryLabel: String?,
    val password: String,
    val importMode: BackupImportModeUiModel,
    val selectedExportFormat: BackupExportFormatUiModel,
    val includeIcons: Boolean,
    val includeAttachments: Boolean,
    val includeDeleted: Boolean,
    val includedEntryTypes: Set<EntryTypeUiModel>,
    val canSubmitExport: Boolean,
)

internal interface BackupRestoreSheetEventHandler {
    fun onDismiss()
    fun onFormatSelected(format: BackupExportFormatUiModel)
    fun onPasswordChanged(password: String)
    fun onIncludeIconsChanged(include: Boolean)
    fun onIncludeAttachmentsChanged(include: Boolean)
    fun onIncludeDeletedChanged(include: Boolean)
    fun onIncludedEntryTypesChanged(types: Set<EntryTypeUiModel>)
    fun onImportModeChanged(mode: BackupImportModeUiModel)
    fun onExportRequested()
    fun onImportRequested()
}
