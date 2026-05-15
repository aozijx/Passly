package com.aozijx.passly.features.settings.internal

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.core.backup.BackupExportStorageSupport
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.security.auth.AuthValidationSupport
import com.aozijx.passly.features.auth.ui.SettingsAuthGateway
import com.aozijx.passly.features.common.toUiMessage

internal enum class AppPasswordAction {
    SET,
    CHANGE,
    DISABLE
}

internal fun SettingsAuthGateway.executeSetAppPassword(
    password: CharArray, context: Context
) {
    setAppPassword(password) { result ->
        result.onSuccess {
            Toast.makeText(context, "应用密码设置成功", Toast.LENGTH_SHORT).show()
        }.onFailure { error ->
            Toast.makeText(context, error.toUiMessage(), Toast.LENGTH_SHORT).show()
        }
    }
}

internal fun SettingsAuthGateway.executeChangeAppPassword(
    oldPassword: CharArray, newPassword: CharArray, context: Context
) {
    changeAppPassword(oldPassword, newPassword) { result ->
        result.onSuccess {
            Toast.makeText(context, "应用密码修改成功", Toast.LENGTH_SHORT).show()
        }.onFailure { error ->
            Toast.makeText(context, error.toUiMessage(), Toast.LENGTH_SHORT).show()
        }
    }
}

internal fun SettingsAuthGateway.executeDisableAppPassword(
    password: CharArray, context: Context
) {
    disableAppPassword(password) { result ->
        result.onSuccess {
            Toast.makeText(context, "应用密码已关闭", Toast.LENGTH_SHORT).show()
        }.onFailure { error ->
            Toast.makeText(context, error.toUiMessage(), Toast.LENGTH_SHORT).show()
        }
    }
}

internal fun SettingsAuthGateway.executeVerifyIdentity(
    activity: FragmentActivity,
    context: Context,
    onVerified: () -> Unit
) {
    verifyIdentity(
        activity = activity,
        title = "身份验证",
        subtitle = "请验证身份以继续操作"
    ) { result ->
        result.onSuccess { onVerified() }
            .onFailure { error ->
                Toast.makeText(context, error.toUiMessage(), Toast.LENGTH_SHORT).show()
            }
    }
}

internal fun SettingsAuthGateway.executeSetLockTimeout(
    timeoutMs: Long,
    context: Context
) {
    val normalized = AuthValidationSupport().normalizeLockTimeout(timeoutMs)
    setAppPassword(charArrayOf()) { result ->
        result.onSuccess {
            Toast.makeText(context, "锁屏时间已更新", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "操作失败", Toast.LENGTH_SHORT).show()
        }
    }
}

internal fun handleInvalidateKeyToggle(
    context: Context,
    activity: FragmentActivity?,
    enabled: Boolean,
    switchPolicy: (FragmentActivity, Boolean, (AppResult<Unit>) -> Unit) -> Unit
) {
    if (activity == null) {
        Toast.makeText(context, "无法进行操作", Toast.LENGTH_SHORT).show()
        return
    }
    switchPolicy(activity, enabled) { result ->
        result.onSuccess {
            Toast.makeText(activity, "安全策略已更新", Toast.LENGTH_SHORT).show()
        }.onFailure { error ->
            Toast.makeText(activity, error.toUiMessage(), Toast.LENGTH_SHORT).show()
        }
    }
}

internal fun handleAppPasswordAction(
    context: Context,
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
                Toast.makeText(context, "两次输入的新密码不一致", Toast.LENGTH_SHORT).show()
                return
            }
            authGateway.setAppPassword(newPassword.toCharArray()) { result ->
                result.onSuccess {
                    Toast.makeText(context, "应用密码设置成功", Toast.LENGTH_SHORT).show()
                    onSuccess(action)
                }.onFailure { error ->
                    Toast.makeText(context, error.toUiMessage(), Toast.LENGTH_SHORT).show()
                }
            }
        }
        AppPasswordAction.CHANGE -> {
            if (currentPassword.isEmpty() || newPassword.isEmpty()) {
                Toast.makeText(context, "请填写所有密码字段", Toast.LENGTH_SHORT).show()
                return
            }
            if (newPassword != confirmPassword) {
                Toast.makeText(context, "两次输入的新密码不一致", Toast.LENGTH_SHORT).show()
                return
            }
            authGateway.changeAppPassword(
                currentPassword.toCharArray(), newPassword.toCharArray()
            ) { result ->
                result.onSuccess {
                    Toast.makeText(context, "应用密码修改成功", Toast.LENGTH_SHORT).show()
                    onSuccess(action)
                }.onFailure { error ->
                    Toast.makeText(context, error.toUiMessage(), Toast.LENGTH_SHORT).show()
                }
            }
        }
        AppPasswordAction.DISABLE -> {
            if (currentPassword.isEmpty()) {
                Toast.makeText(context, "请输入当前密码", Toast.LENGTH_SHORT).show()
                return
            }
            authGateway.disableAppPassword(currentPassword.toCharArray()) { result ->
                result.onSuccess {
                    Toast.makeText(context, "应用密码已关闭", Toast.LENGTH_SHORT).show()
                    onSuccess(action)
                }.onFailure { error ->
                    Toast.makeText(context, error.toUiMessage(), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

internal fun handleAppPasswordEntryClick(
    context: Context,
    activity: FragmentActivity?,
    isAppPasswordEnabled: Boolean,
    authGateway: SettingsAuthGateway,
    title: String,
    subtitle: String,
    authFailedMsg: String,
    onAlreadyEnabled: () -> Unit,
    onVerified: () -> Unit
) {
    if (isAppPasswordEnabled) {
        onAlreadyEnabled()
        return
    }
    if (activity == null) {
        Toast.makeText(context, "无法进行身份验证", Toast.LENGTH_SHORT).show()
        return
    }
    authGateway.verifyIdentity(activity, title, subtitle) { result ->
        result.onSuccess { onVerified() }
            .onFailure { error ->
                val msg = error.toUiMessage(authFailedMsg)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
    }
}

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