package com.aozijx.passly.security.envelope

/**
 * 信封类型（内联值类）。
 *
 * 使用字符串而非 enum，允许插件或未来扩展在**无需重新编译核心模块**的情况下
 * 新增认证方式（如 CompanySSO、LDAP、Azure、CustomHardwareToken 等）。
 *
 * 内置常量覆盖当前支持的所有类型，外部可通过构造函数传入自定义值。
 */
@JvmInline
value class EnvelopeType(val value: String) {
    companion object {
        /** 生物识别（指纹/人脸），Android Keystore 保护 */
        val BIOMETRIC = EnvelopeType("biometric")

        /** 设备锁屏凭据（PIN/Pattern/Password），Android Keystore 保护 */
        val DEVICE_CREDENTIAL = EnvelopeType("device_credential")

        /** 应用密码，Argon2id KDF */
        val APP_PASSWORD = EnvelopeType("app_password")

        /** 恢复码，PBKDF2 KDF */
        val RECOVERY = EnvelopeType("recovery")

        /** 未来：FIDO2 Passkey */
        val PASSKEY = EnvelopeType("passkey")

        /** 未来：YubiKey */
        val YUBIKEY = EnvelopeType("yubikey")

        /** 未来：企业托管的信封 */
        val ENTERPRISE = EnvelopeType("enterprise")
    }
}