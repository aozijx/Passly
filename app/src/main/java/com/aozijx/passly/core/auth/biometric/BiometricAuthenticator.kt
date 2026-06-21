package com.aozijx.passly.core.auth.biometric

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuthenticator {
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String = "",
        cryptoObject: BiometricPrompt.CryptoObject? = null,
        allowDeviceCredentialFallback: Boolean = true,
        onError: ((String) -> Unit)? = null,
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit
    ) {
        val biometricManager = BiometricManager.from(activity)

        val authenticators =
            if (allowDeviceCredentialFallback) {
                BIOMETRIC_STRONG or DEVICE_CREDENTIAL
            } else {
                BIOMETRIC_STRONG
            }

        val canAuthenticate = biometricManager.canAuthenticate(authenticators)
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            val errorMsg = when (canAuthenticate) {
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "设备不支持生物识别"
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "生物识别硬件不可用"
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "请先在系统设置中录入指纹或面部"
                else -> "认证不可用 (错误码: $canAuthenticate)"
            }
            onError?.invoke(errorMsg)
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt =
            BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess(result)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    val error = errString.toString()
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_CANCELED
                    ) {
                        Toast.makeText(activity, error, Toast.LENGTH_SHORT).show()
                    }
                    onError?.invoke(error)
                }

                override fun onAuthenticationFailed() {
                }
            })

        val promptBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        if (cryptoObject != null) {
            if (allowDeviceCredentialFallback) {
                promptBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            } else {
                promptBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG)
                promptBuilder.setNegativeButtonText("取消")
            }
        } else {
            if (allowDeviceCredentialFallback) {
                promptBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            } else {
                promptBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG)
                promptBuilder.setNegativeButtonText("取消")
            }
        }

        runCatching {
            if (cryptoObject != null) {
                biometricPrompt.authenticate(promptBuilder.build(), cryptoObject)
            } else {
                biometricPrompt.authenticate(promptBuilder.build())
            }
        }.onFailure { e ->
            onError?.invoke("启动认证失败: ${e.message}")
        }
    }
}