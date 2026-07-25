package com.aozijx.passly.feature.settings.datamanagement

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.aozijx.passly.core.error.ui.toUiMessage
import com.aozijx.passly.feature.backup.storage.BackupExportStorageSupport

internal fun handleBackupPathPicked(
    context: Context,
    uri: Uri?,
    onResolved: (String) -> Unit
) {
    if (uri == null) {
        Toast.makeText(context, "未选择目录", Toast.LENGTH_SHORT).show()
        return
    }

    // 只持久化系统实际授权的原始 tree URI。Passly 子目录 URI 是派生值，
    // 不能替代授权 URI 写入设置，否则进程重启后精确权限检查会失败。
    try {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    } catch (error: SecurityException) {
        Toast.makeText(
            context,
            error.toUiMessage("无法持久化目录权限，请重新选择"),
            Toast.LENGTH_SHORT
        ).show()
        return
    }

    BackupExportStorageSupport.ensureAppDirectoryTreeUri(context, uri)
        .onSuccess { onResolved(uri.toString()) }
        .onFailure { error ->
            Toast.makeText(context, error.toUiMessage("无法解析目录"), Toast.LENGTH_SHORT).show()
        }
}
