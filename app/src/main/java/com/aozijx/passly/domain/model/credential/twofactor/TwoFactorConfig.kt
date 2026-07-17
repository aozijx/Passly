package com.aozijx.passly.domain.model.credential.twofactor

import com.aozijx.passly.domain.model.credential.twofactor.otp.OtpConfig
import kotlinx.serialization.Serializable

@Serializable
data class TwoFactorConfig(
    val type: TwoFactorType,
    val otp: OtpConfig? = null
)

@Serializable
enum class TwoFactorType {
    TOTP,
    STEAM_GUARD,
    HOTP,
    CUSTOM
}
