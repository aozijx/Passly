package com.aozijx.passly.domain.entry.model.otp

/**
 * OTP 生成失败的类型化错误。
 *
 * 不再返回 "000000"、"INVALID"、"------" 等可能被误认为验证码的字符串。
 */
sealed class OtpGenerationError : Throwable() {
    /** Secret 为空或解码失败（非法 Base32/Base64 字符）。 */
    data object InvalidSecret : OtpGenerationError()

    /** 计数器无效（HOTP counter 为 null 或负值）。 */
    data object InvalidCounter : OtpGenerationError()

    /** 加密/HMAC 运行时错误。 */
    data class CryptoError(override val cause: Throwable) : OtpGenerationError()
}
