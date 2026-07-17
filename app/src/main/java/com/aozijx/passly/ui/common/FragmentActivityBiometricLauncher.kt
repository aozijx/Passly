package com.aozijx.passly.ui.common

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import com.aozijx.passly.feature.auth.biometric.BiometricAuthenticator
import com.aozijx.passly.feature.auth.biometric.BiometricPromptLauncher

/**
 * FragmentActivity 的 BiometricPromptLauncher 实现
 * 用于在 UI 层将 FragmentActivity 适配为 BiometricPromptLauncher 接口
 */
class FragmentActivityBiometricLauncher(
    private val activity: FragmentActivity
) : BiometricPromptLauncher {

    /**
     * 检查 Activity 是否处于可用状态
     */
    fun isActivityValid(): Boolean {
        return !activity.isFinishing &&
                !activity.isDestroyed &&
                activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }

    override fun launchPrompt(
        title: String,
        subtitle: String,
        cryptoObject: BiometricPrompt.CryptoObject?,
        onError: ((Int, String) -> Unit)?,
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit
    ) {
        if (!isActivityValid()) {
            onError?.invoke(-1, "当前页面已关闭，无法进行验证")
            return
        }

        BiometricAuthenticator.authenticate(
            activity = activity,
            title = title,
            subtitle = subtitle,
            cryptoObject = cryptoObject,
            onError = onError,
            onSuccess = onSuccess
        )
    }
}