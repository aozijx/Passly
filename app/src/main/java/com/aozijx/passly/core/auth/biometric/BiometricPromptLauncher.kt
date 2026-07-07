package com.aozijx.passly.core.auth.biometric

import androidx.biometric.BiometricPrompt

/**
 * 生物识别提示启动器接口
 * 用于抽象 UI 层的生物识别认证功能，避免领域层依赖 Android 框架类
 */
interface BiometricPromptLauncher {
    /**
     * 启动生物识别认证
     *
     * @param title 标题
     * @param subtitle 副标题
     * @param cryptoObject 可选的加密对象
     * @param onError 错误回调
     * @param onSuccess 成功回调
     */
    fun launchPrompt(
        title: String,
        subtitle: String,
        cryptoObject: BiometricPrompt.CryptoObject? = null,
        onError: ((Int, String) -> Unit)? = null,
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit
    )
}