package com.aozijx.passly.core.crypto

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * 生物识别辅助类：封装了指纹、面部及设备密码的认证逻辑
 */
object BiometricHelper {
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String = "",
        cryptoObject: BiometricPrompt.CryptoObject? = null,
        onError: ((String) -> Unit)? = null,
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit
    ) {
        val biometricManager = BiometricManager.from(activity)
        
        // 检查是否有可用的认证方式
        val canAuthenticate = biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            val errorMsg = when (canAuthenticate) {
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "设备不支持生物识别"
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "生物识别硬件不可用"
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    showSetupScreenLockDialog(activity)
                    "请先在系统设置中设置屏幕锁"
                }
                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "需要安全更新"
                else -> "认证不可用"
            }
            if (canAuthenticate != BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                Toast.makeText(activity, errorMsg, Toast.LENGTH_LONG).show()
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
                    val errorMsg = errString.toString()
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_CANCELED
                    ) {
                        Toast.makeText(activity, "验证: $errorMsg", Toast.LENGTH_SHORT).show()
                    }
                    onError?.invoke(errorMsg)
                }

                override fun onAuthenticationFailed() {
                    Toast.makeText(activity, "验证未识别，请重试", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        if (cryptoObject != null) {
            biometricPrompt.authenticate(promptInfo, cryptoObject)
        } else {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    private fun showSetupScreenLockDialog(activity: FragmentActivity) {
        AlertDialog.Builder(activity)
            .setTitle("需要设置屏幕锁")
            .setMessage("为了保护您的数据安全，需要先在系统设置中设置屏幕锁（PIN、图案或密码）。")
            .setPositiveButton("去设置") { _, _ ->
                val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                activity.startActivity(intent)
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
