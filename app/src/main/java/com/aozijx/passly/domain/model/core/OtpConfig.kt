package com.aozijx.passly.domain.model.core

import kotlinx.serialization.Serializable

/**
 * OTP 类型。
 */
@Serializable
enum class OtpType {
    TOTP,
    HOTP,
    STEAM
}

/**
 * 哈希算法。
 */
@Serializable
enum class OtpHashAlgorithm {
    SHA1,
    SHA256,
    SHA512
}

/**
 * Secret 编码方式。
 */
@Serializable
enum class OtpSecretEncoding {
    BASE32,
    BASE64
}

/**
 * 纯业务的 OTP 配置模型。
 *
 * - TOTP 使用 [periodSeconds]（默认 30），[counter] 为 null
 * - HOTP 使用 [counter]，[periodSeconds] 为 null，生成后调用方需持久化递增后的 counter
 * - STEAM 是 [OtpType.STEAM] 类型，默认 SHA1、5 位、30 秒周期、Steam 字符表
 */
@Serializable
data class OtpConfig(
    val type: OtpType = OtpType.TOTP,
    val secret: String,
    val algorithm: OtpHashAlgorithm = OtpHashAlgorithm.SHA1,
    val digits: Int = 6,
    /** TOTP 周期（秒），仅 TOTP/STEAM 有效。 */
    val periodSeconds: Int? = 30,
    /** HOTP 计数器，仅 HOTP 有效。 */
    val counter: Long? = null,
    val encoding: OtpSecretEncoding = OtpSecretEncoding.BASE32,
    val issuer: String? = null,
    val accountName: String? = null
) {
    companion object {
        /** Steam 默认配置。 */
        fun steamDefaults(secret: String, issuer: String? = null): OtpConfig = OtpConfig(
            type = OtpType.STEAM,
            secret = secret,
            algorithm = OtpHashAlgorithm.SHA1,
            digits = 5,
            periodSeconds = 30,
            encoding = OtpSecretEncoding.BASE32,
            issuer = issuer
        )
    }
}
