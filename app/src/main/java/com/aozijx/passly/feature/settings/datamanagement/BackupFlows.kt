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

    // 持久化目录访问权限，避免进程重启后权限丢失
    try {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    } catch (_: SecurityException) {
        // 部分设备或路径可能不支持持久化权限，不阻塞流程
    }

    BackupExportStorageSupport.ensureAppDirectoryTreeUri(context, uri)
        .onSuccess { resolvedUri -> onResolved(resolvedUri.toString()) }
        .onFailure { error ->
            Toast.makeText(context, error.toUiMessage("无法解析目录"), Toast.LENGTH_SHORT).show()
        }
}
