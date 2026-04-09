package com.aozijx.passly.features.settings.internal

import android.content.Context
import android.net.Uri
import com.aozijx.passly.R
import com.aozijx.passly.core.backup.BackupImportMode
import com.aozijx.passly.core.backup.BackupManager
import com.aozijx.passly.service.backup.BackupImportIconSyncForegroundService

internal data class BackupActionOutcome(
    val message: String, val isFailure: Boolean
)

internal class BackupActionSupport {

    suspend fun runBackupAction(
        context: Context,
        uri: Uri,
        password: CharArray,
        isExporting: Boolean,
        importMode: BackupImportMode,
        includeImages: Boolean // 新增参数
    ): BackupActionOutcome {
        val result = if (isExporting) {
            BackupManager.exportBackup(context, uri, password, includeImages)
        } else {
            BackupManager.importBackup(context, uri, password, importMode)
        }

        return if (result.isSuccess) {
            if (isExporting) {
                BackupActionOutcome(
                    message = context.getString(R.string.backup_action_export_success),
                    isFailure = false
                )
            } else {
                BackupImportIconSyncForegroundService.start(context)
                BackupActionOutcome(
                    message = context.getString(R.string.backup_action_import_success),
                    isFailure = false
                )
            }
        } else {
            BackupActionOutcome(
                message = buildFailureMessage(context, result.exceptionOrNull()), isFailure = true
            )
        }
    }

    private fun buildFailureMessage(context: Context, error: Throwable?): String {
        val message = error?.message?.trim().orEmpty()
        return when {
            message.contains("密码错误") -> context.getString(R.string.backup_error_password_incorrect)
            message.contains("不是有效的 Passly 备份文件") -> context.getString(R.string.backup_error_invalid_file)
            message.contains("不支持的备份版本") -> context.getString(R.string.backup_error_incompatible_version)
            message.contains("文件损坏") || message.contains("data.json") -> context.getString(R.string.backup_error_corrupted)
            message.isNotEmpty() -> context.getString(R.string.backup_error_with_reason, message)
            else -> context.getString(R.string.backup_error_unknown)
        }
    }
}
