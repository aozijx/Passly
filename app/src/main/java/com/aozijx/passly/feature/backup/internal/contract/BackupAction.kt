package com.aozijx.passly.feature.backup.internal.contract

import android.net.Uri
import com.aozijx.passly.domain.backup.model.BackupExportUiFormat
import com.aozijx.passly.domain.backup.model.ImportMode
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.sensitive.SensitiveValue

/** User and host events accepted by the Backup state machine. */
sealed interface BackupAction {
    data class CheckDirectoryPermission(val uri: String?) : BackupAction

    data class PrepareExport(val format: BackupExportUiFormat) : BackupAction

    data class StartExport(
        val uri: Uri,
        val deleteOnFailure: Boolean = false,
    ) : BackupAction

    data object StartExportInConfiguredDirectory : BackupAction
    data class StartImport(val uri: Uri) : BackupAction

    data class UpdatePassword(val password: SensitiveValue) : BackupAction
    data class UpdateImportMode(val mode: ImportMode) : BackupAction
    data class UpdateIncludeIcons(val include: Boolean) : BackupAction
    data class UpdateIncludeAttachments(val include: Boolean) : BackupAction
    data class UpdateIncludeDeleted(val include: Boolean) : BackupAction
    data class UpdateIncludedEntryTypes(val types: Set<EntryType>) : BackupAction

    data object CancelPendingOperation : BackupAction
    data object ProcessBackupAction : BackupAction
}
