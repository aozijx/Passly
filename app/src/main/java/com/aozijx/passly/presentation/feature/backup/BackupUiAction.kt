package com.aozijx.passly.presentation.feature.backup

import android.net.Uri
import com.aozijx.passly.feature.backup.internal.model.BackupExportFormat
import com.aozijx.passly.feature.backup.internal.model.ImportMode
import com.aozijx.passly.presentation.shared.entry.EntryTypeUiModel

/** User and host events accepted by the Backup state machine. */
sealed interface BackupUiAction {
    data class CheckDirectoryPermission(val uri: String?) : BackupUiAction
    data object OpenExportOptions : BackupUiAction
    data object RequestImportDocument : BackupUiAction
    data class SelectExportFormat(val format: BackupExportFormat) : BackupUiAction

    data class StartExport(
        val uri: Uri,
        val deleteOnFailure: Boolean = false,
    ) : BackupUiAction

    data class StartImport(val uri: Uri) : BackupUiAction

    data class UpdatePassword(val password: String) : BackupUiAction
    data class UpdateImportMode(val mode: ImportMode) : BackupUiAction
    data class UpdateIncludeIcons(val include: Boolean) : BackupUiAction
    data class UpdateIncludeAttachments(val include: Boolean) : BackupUiAction
    data class UpdateIncludeDeleted(val include: Boolean) : BackupUiAction
    data class UpdateIncludedEntryTypes(val types: Set<EntryTypeUiModel>) : BackupUiAction

    data class SubmitExport(val useConfiguredDirectory: Boolean) : BackupUiAction
    data object SubmitImport : BackupUiAction
    data object DismissOptions : BackupUiAction
}
