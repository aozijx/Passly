package com.aozijx.passly.core.error.auth

import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.core.error.AuthFailed
import com.aozijx.passly.core.error.BiometricLockedOut
import com.aozijx.passly.core.error.BiometricNotEnrolled
import com.aozijx.passly.core.error.BiometricUnavailable
import com.aozijx.passly.core.error.CryptoDataCorrupted
import com.aozijx.passly.core.error.ErrorLayer
import com.aozijx.passly.core.error.ErrorTrace
import com.aozijx.passly.core.error.KeyStateError
import com.aozijx.passly.core.log.Logcat
import com.aozijx.passly.security.crypto.DekManager
import com.aozijx.passly.security.crypto.SessionManager
import java.security.InvalidKeyException
import java.security.KeyStoreException
import javax.crypto.BadPaddingException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthErrorHandler @Inject constructor(
    private val dekManager: DekManager
) {
    private companion object {
        private const val TAG = "AuthErrorHandler"
    }

    fun classifyError(throwable: Throwable, operation: String): AppError {
        val trace = ErrorTrace(originLayer = ErrorLayer.DATA, operation = operation)
        return when (throwable) {
            is InvalidKeyException ->
                KeyStateError(
                    message = "无效加密密钥",
                    detail = operation,
                    trace = trace
                )

            is BadPaddingException ->
                CryptoDataCorrupted(
                    message = "解密失败，数据可能损坏",
                    detail = "BadPadding",
                    trace = trace
                )

            is KeyStoreException ->
                KeyStateError(
                    message = "KeyStore 异常",
                    detail = operation,
                    trace = trace
                )

            is IllegalStateException ->
                KeyStateError(
                    message = throwable.message ?: "状态异常",
                    detail = operation,
                    trace = trace
                )

            is SecurityException ->
                AuthFailed(
                    message = throwable.message ?: "权限不足",
                    trace = trace
                )

            else ->
                AuthFailed(
                    message = throwable.message ?: "认证失败",
                    trace = trace,
                    cause = throwable
                )
        }
    }

    /**
     * 根据 Android BiometricPrompt 错误码分类为 [AppError] 子类。
     *
     * 错误码对照：
     * 1  = HW_UNAVAILABLE     5  = CANCELED
     * 7  = LOCKOUT             8  = VENDOR
     * 9  = LOCKOUT_PERMANENT  10 = USER_CANCELED
     * 11 = NO_BIOMETRICS      12 = HW_NOT_PRESENT
     * 13 = NEGATIVE_BUTTON
     */
    fun classifyBiometricError(errorCode: Int, errorMessage: String): AppError {
        val trace = ErrorTrace(
            originLayer = ErrorLayer.DATA,
            operation = "biometric_prompt",
            extras = mapOf("errorCode" to errorCode.toString(), "errorMessage" to errorMessage)
        )
        return when (errorCode) {
            // ── 用户取消：不视为错误，仅传递给调用方 ──
            5, 10, 13 -> AuthFailed(
                message = "认证已取消",
                trace = trace
            )

            // ── 硬件/传感器不适配 ──
            11, 12 -> BiometricUnavailable(
                message = "设备不支持生物识别",
                trace = trace
            )

            1 -> BiometricUnavailable(
                message = "生物识别硬件暂不可用",
                trace = trace
            )

            // ── 锁定 ──
            7 -> BiometricLockedOut(
                lockoutDurationMs = 30_000,
                message = "生物识别已锁定，请等待 30 秒后重试",
                trace = trace
            )

            9 -> BiometricLockedOut(
                lockoutDurationMs = 0,
                message = "生物识别已永久锁定，请使用密码",
                trace = trace
            )

            // ── 其他所有 ──
            else -> AuthFailed(
                message = errorMessage.ifEmpty { "生物识别认证失败" },
                trace = trace
            )
        }
    }

    /**
     * 统一处理认证失败：日志 + 清理敏感状态。
     */
    suspend fun handleAuthFailure(error: AppError, operation: String) {
        Logcat.e(TAG, "Auth failure in $operation [${error.code}]: ${error.message}", error)

        cleanupSensitiveState()

        // 详细日志
        when (error) {
            is BiometricLockedOut ->
                Logcat.w(TAG, "Biometric locked out for ${error.lockoutDurationMs}ms")

            is KeyStateError ->
                Logcat.e(TAG, "Key state error: detail=${error.detail}")

            is CryptoDataCorrupted ->
                Logcat.e(TAG, "Crypto data corrupted: ${error.detail}")

            is BiometricUnavailable ->
                Logcat.w(TAG, "Biometric unavailable: ${error.message}")

            is BiometricNotEnrolled ->
                Logcat.w(TAG, "Biometric not enrolled: ${error.message}")

            else -> Unit
        }
    }

    suspend fun cleanupSensitiveState() {
        try {
            dekManager.lock()
            SessionManager.clearSessionKey()
            Logcat.i(TAG, "Sensitive state cleared")
        } catch (e: Exception) {
            Logcat.e(TAG, "Failed to clear sensitive state", e)
        }
    }
}