package com.aozijx.passly.feature.backup.contract

import android.net.Uri
import com.aozijx.passly.domain.model.backup.ImportMode

sealed class BackupIntent {
    data class CheckDirectoryPermission(val uri: String?) : BackupIntent()
    data class SetBackupDirectoryUri(val uri: String) : BackupIntent()
    data object ClearBackupDirectoryUri : BackupIntent()

    data class StartExport(
        val uri: Uri,
        val fileNameHint: String? = null,
        val allowFallback: Boolean = false
    ) : BackupIntent()

    data class StartImport(val uri: Uri) : BackupIntent()

    data class UpdatePassword(val password: String) : BackupIntent()
    data class UpdateImportMode(val mode: ImportMode) : BackupIntent()
    data class UpdateIncludeIcons(val include: Boolean) : BackupIntent()

    data object DismissPasswordDialog : BackupIntent()
    data object ResetBackupStatus : BackupIntent()

    data class TryStartExportInConfiguredDirectory(val directoryUri: String?) : BackupIntent()

    /// 触发备份/恢复操作（ViewModel 内部验证状态后，通过 Effect 请求认证）
    data object ProcessBackupAction : BackupIntent()

    /// 认证成功后，由 UI 层发送，ViewModel 执行实际操作
    data object ExecuteBackup : BackupIntent()

    data class ExportPlainBackup(val dirUri: String?) : BackupIntent()
    data class ExportPlainBackupToUri(val uri: Uri) : BackupIntent()
    data class ExportTextBackup(val uri: Uri) : BackupIntent()

    data object IssuePlainExportToken : BackupIntent()
}
