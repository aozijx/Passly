package com.aozijx.passly.domain.model.credential.twofactor.otp

import kotlinx.serialization.Serializable

/**
 * 纯业务的 OTP 配置模型。
 */
@Serializable
data class OtpConfig(
    val secret: String,
    val digits: Int,
    val period: Int,
    val algorithm: String,
    val issuer: String? = null,
    val label: String? = null
)

enum class OtpType {
    TOTP,
    HOTP,
    STEAM_GUARD
}
