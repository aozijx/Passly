package com.aozijx.passly.core.auth.error

import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.security.crypto.DekManager
import com.aozijx.passly.security.crypto.SessionManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 统一的认证错误处理器，负责：
 * 1. 将异常转换为 AuthError 分类
 * 2. 集中清理敏感状态
 * 3. 记录日志
 */
@Singleton
class AuthErrorHandler @Inject constructor(
    private val dekManager: DekManager
) {
    private companion object {
        private const val TAG = "AuthErrorHandler"
    }

    /**
     * 将 Throwable 转换为 AuthError。
     */
    fun classifyError(throwable: Throwable, operation: String): AuthError {
        return when (throwable) {
            is java.security.InvalidKeyException ->
                AuthError.KeyStateError(operation, "无效加密密钥")

            is javax.crypto.BadPaddingException ->
                AuthError.CryptoDataCorrupted("解密失败，数据可能损坏")

            is java.security.KeyStoreException ->
                AuthError.KeyStateError(operation, "KeyStore 异常")

            is IllegalStateException ->
                AuthError.KeyStateError(operation, throwable.message ?: "状态异常")

            is SecurityException ->
                AuthError.InvalidAuthRequest(throwable.message ?: "权限不足")

            else -> AuthError.Unknown(throwable)
        }
    }

    /**
     * 处理认证失败，清理敏感状态并记录日志。
     */
    suspend fun handleAuthFailure(error: AuthError, operation: String) {
        Logcat.e(TAG, "Auth failure in $operation: ${error.toUserMessage()}")

        // 清理敏感状态
        cleanupSensitiveState()

        // 记录详细日志
        when (error) {
            is AuthError.BiometricLockedOut ->
                Logcat.w(TAG, "Biometric locked out for ${error.lockoutDurationMs}ms")

            is AuthError.KeyStateError ->
                Logcat.e(
                    TAG,
                    "Key state error: operation=${error.operation}, detail=${error.detail}"
                )

            is AuthError.CryptoDataCorrupted ->
                Logcat.e(TAG, "Crypto data corrupted: ${error.detail}")

            is AuthError.Unknown ->
                Logcat.e(TAG, "Unknown error", error.throwable)

            else -> Unit
        }
    }

    /**
     * 集中清理敏感状态。
     */
    suspend fun cleanupSensitiveState() {
        try {
            dekManager.lock()
            SessionManager.clearSessionKey()
            Logcat.i(TAG, "Sensitive state cleared")
        } catch (e: Exception) {
            Logcat.e(TAG, "Failed to clear sensitive state", e)
        }
    }

    /**
     * 生物识别错误码转换为 AuthError。
     */
    fun classifyBiometricError(errorCode: Int, errorMessage: String): AuthError {
        return when (errorCode) {
            // BiometricPrompt 常见错误码
            10 -> AuthError.BiometricHardwareUnavailable(errorMessage) // BIOMETRIC_ERROR_HW_UNAVAILABLE
            11 -> AuthError.BiometricHardwareUnavailable(errorMessage) // BIOMETRIC_ERROR_HW_NOT_PRESENT
            12 -> AuthError.BiometricNotEnrolled(errorMessage) // BIOMETRIC_ERROR_NO_BIOMETRICS
            13 -> AuthError.BiometricNotEnrolled(errorMessage) // BIOMETRIC_ERROR_HW_NOT_PRESENT
            7 -> AuthError.BiometricLockedOut(30_000) // BIOMETRIC_ERROR_LOCKOUT (30秒)
            8 -> AuthError.BiometricLockedOut(300_000) // BIOMETRIC_ERROR_LOCKOUT_PERMANENT (5分钟)
            else -> AuthError.BiometricAuthFailed(errorCode, errorMessage)
        }
    }
}