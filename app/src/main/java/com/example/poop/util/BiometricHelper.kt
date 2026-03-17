package com.example.poop.utils

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {
    /**
     * 通用的生物识别认证方法，支持指纹/面部以及设备密码（PIN/图案/密码）
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String = "",
        cryptoObject: BiometricPrompt.CryptoObject? = null,
        onError: ((String) -> Unit)? = null,
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt =
            BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess(result)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    val errorMsg = errString.toString()
                    // 用户取消或点击负面按钮通常不需要显示 Toast
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_CANCELED
                    ) {
                        Toast.makeText(activity, "验证错误: $errorMsg", Toast.LENGTH_SHORT).show()
                    }
                    onError?.invoke(errorMsg)
                }

                override fun onAuthenticationFailed() {
                    Toast.makeText(activity, "验证未识别，请重试", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        // 允许使用生物识别（强）和设备凭据（PIN/密码/图案）
        // 注意：如果设置了 DEVICE_CREDENTIAL，则不能调用 setNegativeButtonText
        promptInfoBuilder.setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        val promptInfo = promptInfoBuilder.build()

        if (cryptoObject != null) {
            biometricPrompt.authenticate(promptInfo, cryptoObject)
        } else {
            biometricPrompt.authenticate(promptInfo)
        }
    }
}