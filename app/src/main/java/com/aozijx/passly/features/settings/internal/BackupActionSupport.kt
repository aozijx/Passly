package com.aozijx.passly.features.settings.internal

import android.content.Context
import android.net.Uri
import com.aozijx.passly.core.backup.BackupManager

internal class BackupActionSupport {

    suspend fun runBackupAction(
        context: Context,
        uri: Uri,
        password: CharArray,
        isExporting: Boolean,
        importMode: BackupManager.ImportMode
    ): String {
        val result = if (isExporting) {
            BackupManager.exportBackup(context, uri, password)
        } else {
            BackupManager.importBackup(context, uri, password, importMode)
        }

        return if (result.isSuccess) {
            if (isExporting) "导出成功" else "导入成功"
        } else {
            buildFailureMessage(result.exceptionOrNull())
        }
    }

    private fun buildFailureMessage(error: Throwable?): String {
        val message = error?.message?.trim().orEmpty()
        if (message.contains("密码错误")) {
            return message
        }
        return if (message.isNotEmpty()) {
            "失败: $message"
        } else {
            "失败: 未知错误"
        }
    }
}
