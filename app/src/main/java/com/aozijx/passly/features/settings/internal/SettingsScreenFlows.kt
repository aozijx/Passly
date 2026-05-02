package com.aozijx.passly.features.settings.internal

import android.net.Uri
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.core.backup.BackupExportStorageSupport
import com.aozijx.passly.features.auth.ui.SettingsAuthGateway

internal enum class AppPasswordAction {
    SET,
    CHANGE,
    DISABLE
}

internal fun handleAppPasswordAction(
    context: android.content.Context,
    action: AppPasswordAction,
    currentPassword: String,
    newPassword: String,
    confirmPassword: String,
    authGateway: SettingsAuthGateway,
    onSuccess: (AppPasswordAction) -> Unit
) {
    when (action) {
        AppPasswordAction.SET -> {
            if (newPassword != confirmPassword) {
                Toast.makeText(context, "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
                return
            }
            val newPasswordChars = newPassword.toCharArray()
            authGateway.setAppPassword(newPasswordChars) { result ->
                try {
                    result.onSuccess {
                        Toast.makeText(context, "应用密码已启用", Toast.LENGTH_SHORT).show()
                        onSuccess(AppPasswordAction.SET)
                    }.onFailure { e ->
                        Toast.makeText(context, e.message ?: "设置失败", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    newPasswordChars.fill('\u0000')
                }
            }
        }

        AppPasswordAction.CHANGE -> {
            if (newPassword != confirmPassword) {
                Toast.makeText(context, "两次输入的新密码不一致", Toast.LENGTH_SHORT).show()
                return
            }
            val oldPasswordChars = currentPassword.toCharArray()
            val newPasswordChars = newPassword.toCharArray()
            authGateway.changeAppPassword(
                oldPassword = oldPasswordChars,
                newPassword = newPasswordChars
            ) { result ->
                try {
                    result.onSuccess {
                        Toast.makeText(context, "应用密码已更新", Toast.LENGTH_SHORT).show()
                        onSuccess(AppPasswordAction.CHANGE)
                    }.onFailure { e ->
                        Toast.makeText(context, e.message ?: "修改失败", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    oldPasswordChars.fill('\u0000')
                    newPasswordChars.fill('\u0000')
                }
            }
        }

        AppPasswordAction.DISABLE -> {
            val currentPasswordChars = currentPassword.toCharArray()
            authGateway.disableAppPassword(currentPasswordChars) { result ->
                try {
                    result.onSuccess {
                        Toast.makeText(context, "已关闭应用密码", Toast.LENGTH_SHORT).show()
                        onSuccess(AppPasswordAction.DISABLE)
                    }.onFailure { e ->
                        Toast.makeText(context, e.message ?: "关闭失败", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    currentPasswordChars.fill('\u0000')
                }
            }
        }
    }
}

internal fun verifyBeforeSetAppPassword(
    context: android.content.Context,
    activity: FragmentActivity?,
    authGateway: SettingsAuthGateway,
    title: String,
    subtitle: String,
    authFailedMsg: String,
    onVerified: () -> Unit
) {
    if (activity == null) {
        Toast.makeText(context, "当前页面不支持验证", Toast.LENGTH_SHORT).show()
        return
    }

    authGateway.verifyIdentity(
        activity = activity,
        title = title,
        subtitle = subtitle
    ) { result ->
        result.onSuccess {
            onVerified()
        }.onFailure { e ->
            Toast.makeText(context, e.message ?: authFailedMsg, Toast.LENGTH_SHORT).show()
        }
    }
}

internal fun handleBackupPathPicked(
    context: android.content.Context,
    uri: Uri?,
    onPicked: (String) -> Unit
) {
    if (uri == null) return
    val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    runCatching<Unit> {
        context.contentResolver.takePersistableUriPermission(uri, flags)
    }
    val appDirectoryTreeUri =
        BackupExportStorageSupport.ensureAppDirectoryTreeUri(context, uri).getOrElse {
            Toast.makeText(context, "目录初始化失败，请重新选择", Toast.LENGTH_SHORT).show()
            return
        }
    onPicked(appDirectoryTreeUri.toString())
}