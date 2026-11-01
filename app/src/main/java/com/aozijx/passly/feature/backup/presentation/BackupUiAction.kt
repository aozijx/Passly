package com.aozijx.passly.feature.backup.presentation

import com.aozijx.passly.feature.backup.contract.BackupExportFormat
import com.aozijx.passly.feature.backup.contract.BackupImportStrategy
import com.aozijx.passly.feature.backup.contract.ImportMode
import com.aozijx.passly.domain.entry.model.EntryType

/** User and host events accepted by the Backup state machine. */
sealed interface BackupUiAction {
    data class CheckDirectoryPermission(val uri: String?) : BackupUiAction
    data object OpenExportOptions : BackupUiAction
    data class RequestImportDocument(val pageOwner: Any) : BackupUiAction
    data class SelectExportFormat(val format: BackupExportFormat) : BackupUiAction

    data class StartExport(
        val uri: String?,
        val pageOwner: Any,
        val requestId: String,
        val deleteOnFailure: Boolean = false,
    ) : BackupUiAction

    data class StartImport(
        val uri: String?,
        val pageOwner: Any,
        val requestId: String,
    ) : BackupUiAction

    data class UpdatePassword(val password: String) : BackupUiAction
    data class UpdateImportMode(val mode: ImportMode) : BackupUiAction
    data class UpdateImportStrategy(val strategy: BackupImportStrategy) : BackupUiAction
    data class UpdateIncludeIcons(val include: Boolean) : BackupUiAction
    data class UpdateIncludeAttachments(val include: Boolean) : BackupUiAction
    data class UpdateIncludeDeleted(val include: Boolean) : BackupUiAction
    data class UpdateIncludedEntryTypes(val types: Set<EntryType>) : BackupUiAction

    data class SubmitExport(val useConfiguredDirectory: Boolean) : BackupUiAction
    data object SubmitImport : BackupUiAction
    data object DismissOptions : BackupUiAction
}
