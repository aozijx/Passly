package com.aozijx.passly.features.settings.data

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.aozijx.passly.core.backup.BackupExportStorageSupport
import com.aozijx.passly.features.common.toUiMessage

internal fun handleBackupPathPicked(
    context: Context,
    uri: Uri?,
    onResolved: (String) -> Unit
) {
    if (uri == null) {
        Toast.makeText(context, "未选择目录", Toast.LENGTH_SHORT).show()
        return
    }
    BackupExportStorageSupport.ensureAppDirectoryTreeUri(context, uri)
        .onSuccess { resolvedUri -> onResolved(resolvedUri.toString()) }
        .onFailure { error ->
            Toast.makeText(context, error.toUiMessage("无法解析目录"), Toast.LENGTH_SHORT).show()
        }
}