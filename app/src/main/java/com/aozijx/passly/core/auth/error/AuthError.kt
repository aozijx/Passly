package com.aozijx.passly.core.auth.error

import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.core.error.ErrorLayer
import com.aozijx.passly.core.error.ErrorTrace

/**
 * 认证错误分类，每种错误对应具体的 UI 提示和恢复策略。
 */
sealed class AuthError {
    /** 生物识别硬件不可用 */
    data class BiometricHardwareUnavailable(val reason: String) : AuthError()

    /** 未录入生物识别 */
    data class BiometricNotEnrolled(val message: String) : AuthError()

    /** 生物识别认证失败（用户取消/错误） */
    data class BiometricAuthFailed(val errorCode: Int?, val message: String) : AuthError()

    /** 生物识别认证被锁定（多次失败） */
    data class BiometricLockedOut(val lockoutDurationMs: Long) : AuthError()

    /** App Password 不正确 */
    data class AppPasswordIncorrect(val attemptCount: Int) : AuthError()

    /** App Password 未启用 */
    data class AppPasswordNotEnabled(val message: String) : AuthError()

    /** App Password 设置失败 */
    data class AppPasswordSetupFailed(val reason: String) : AuthError()

    /** 密钥状态异常 */
    data class KeyStateError(val operation: String, val detail: String) : AuthError()

    /** 加密数据损坏 */
    data class CryptoDataCorrupted(val detail: String) : AuthError()

    /** 认证请求无效 */
    data class InvalidAuthRequest(val message: String) : AuthError()

    /** 应用已锁定，需要先解锁 */
    data class AppLocked(val message: String) : AuthError()

    /** 未知错误 */
    data class Unknown(val throwable: Throwable?) : AuthError()

    /**
     * 获取用户可读的错误提示。
     */
    fun toUserMessage(): String = when (this) {
        is BiometricHardwareUnavailable -> "设备不支持生物识别: $reason"
        is BiometricNotEnrolled -> "请先在系统设置中录入指纹或面部"
        is BiometricAuthFailed -> message.ifEmpty { "生物识别认证失败" }
        is BiometricLockedOut -> "生物识别已锁定，请等待 ${lockoutDurationMs / 1000} 秒后重试"
        is AppPasswordIncorrect -> "密码不正确"
        is AppPasswordNotEnabled -> message.ifEmpty { "应用密码未启用" }
        is AppPasswordSetupFailed -> "密码设置失败: $reason"
        is KeyStateError -> "认证状态异常，请重试"
        is CryptoDataCorrupted -> "加密数据损坏，请联系支持"
        is InvalidAuthRequest -> message
        is AppLocked -> message.ifEmpty { "请先解锁应用" }
        is Unknown -> throwable?.message ?: "认证过程中发生未知错误"
    }

    /**
     * 是否可以自动降级到备选认证方式。
     */
    fun canFallback(): Boolean = when (this) {
        is BiometricHardwareUnavailable,
        is BiometricNotEnrolled,
        is BiometricAuthFailed,
        is BiometricLockedOut -> true

        else -> false
    }

    /**
     * 获取建议的降级认证方式。
     */
    fun suggestedFallback(): FallbackAuthMethod? = when (this) {
        is BiometricHardwareUnavailable,
        is BiometricNotEnrolled,
        is BiometricLockedOut -> FallbackAuthMethod.APP_PASSWORD

        is BiometricAuthFailed -> FallbackAuthMethod.APP_PASSWORD

        else -> null
    }
}

enum class FallbackAuthMethod {
    APP_PASSWORD
}

/**
 * 将 AuthError 转换为 AppError.AuthFailed。
 */
fun AuthError.toAppError(): AppError.AuthFailed {
    return AppError.AuthFailed(
        message = toUserMessage(),
        errorTrace = ErrorTrace(originLayer = ErrorLayer.DATA),
        cause = when (this) {
            is AuthError.Unknown -> throwable
            else -> null
        }
    )
}