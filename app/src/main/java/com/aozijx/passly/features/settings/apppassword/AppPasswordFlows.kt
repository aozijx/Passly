package com.aozijx.passly.features.settings.apppassword

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.features.common.toUiMessage
import com.aozijx.passly.features.verification.contract.VerificationGateway

enum class AppPasswordAction {
    SET,
    CHANGE,
    DISABLE
}

internal fun handleAppPasswordAction(
    context: Context,
    action: AppPasswordAction,
    currentPassword: String,
    newPassword: String,
    confirmPassword: String,
    authGateway: VerificationGateway,
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
    authGateway: VerificationGateway,
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
    authGateway.verifyWithBiometric(activity, title, subtitle) { result ->
        result.onSuccess { onVerified() }
            .onFailure { error ->
                val msg = error.toUiMessage(authFailedMsg)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
    }
}