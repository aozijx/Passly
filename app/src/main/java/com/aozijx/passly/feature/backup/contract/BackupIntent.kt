package com.aozijx.passly.feature.backup.contract

import android.net.Uri
import com.aozijx.passly.domain.backup.model.ImportMode
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.backup.model.BackupExportUiFormat

sealed interface BackupIntent {
    data class CheckDirectoryPermission(val uri: String?) : BackupIntent

    data class PrepareExport(val format: BackupExportUiFormat) : BackupIntent

    /** Prepare the recovery-code-only path. This always exports encrypted Passly format. */
    data object PrepareRecoveryExport : BackupIntent

    data class StartExport(
        val uri: Uri,
        val fileNameHint: String? = null,
        val deleteOnFailure: Boolean = false
    ) : BackupIntent

    data object StartExportInConfiguredDirectory : BackupIntent
    data class StartImport(val uri: Uri) : BackupIntent

    data class UpdatePassword(val password: String) : BackupIntent
    data class UpdateImportMode(val mode: ImportMode) : BackupIntent
    data class UpdateIncludeIcons(val include: Boolean) : BackupIntent
    data class UpdateIncludeAttachments(val include: Boolean) : BackupIntent
    data class UpdateIncludeDeleted(val include: Boolean) : BackupIntent
    data class UpdateIncludedEntryTypes(val types: Set<EntryType>) : BackupIntent

    data object CancelPendingOperation : BackupIntent
    data object ProcessBackupAction : BackupIntent
}
