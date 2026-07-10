package com.aozijx.passly.core.error.auth

import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.core.error.AppPasswordIncorrect
import com.aozijx.passly.core.error.BiometricLockedOut
import com.aozijx.passly.core.error.BiometricNotEnrolled
import com.aozijx.passly.core.error.BiometricUnavailable

/**
 * 认证降级方案。
 */
enum class FallbackAuthMethod {
    APP_PASSWORD
}

/**
 * 生物识别相关错误是否可以降级到备选认证方式（应用密码）。
 * 纯类型判断，不依赖 message 文案。
 */
fun AppError.canFallbackForAuth(): Boolean {
    return this is BiometricUnavailable
            || this is BiometricNotEnrolled
            || this is BiometricLockedOut
            || this is AppPasswordIncorrect
}

/**
 * 获取建议的降级认证方式。
 */
fun AppError.suggestedAuthFallback(): FallbackAuthMethod? {
    return if (canFallbackForAuth()) FallbackAuthMethod.APP_PASSWORD else null
}