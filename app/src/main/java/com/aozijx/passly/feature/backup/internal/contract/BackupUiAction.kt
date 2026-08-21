package com.aozijx.passly.feature.backup.internal.contract

import android.net.Uri
import com.aozijx.passly.feature.backup.internal.model.BackupExportUiFormat
import com.aozijx.passly.feature.backup.internal.model.ImportMode
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.sensitive.SensitiveValue

/** User and host events accepted by the Backup state machine. */
sealed interface BackupUiAction {
    data class CheckDirectoryPermission(val uri: String?) : BackupUiAction

    data class PrepareExport(val format: BackupExportUiFormat) : BackupUiAction

    data class StartExport(
        val uri: Uri,
        val deleteOnFailure: Boolean = false,
    ) : BackupUiAction

    data object StartExportInConfiguredDirectory : BackupUiAction
    data class StartImport(val uri: Uri) : BackupUiAction

    data class UpdatePassword(val password: SensitiveValue) : BackupUiAction
    data class UpdateImportMode(val mode: ImportMode) : BackupUiAction
    data class UpdateIncludeIcons(val include: Boolean) : BackupUiAction
    data class UpdateIncludeAttachments(val include: Boolean) : BackupUiAction
    data class UpdateIncludeDeleted(val include: Boolean) : BackupUiAction
    data class UpdateIncludedEntryTypes(val types: Set<EntryType>) : BackupUiAction

    data object CancelPendingOperation : BackupUiAction
    data object ProcessBackupAction : BackupUiAction
}
