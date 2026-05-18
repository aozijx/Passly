package com.aozijx.passly.core.auth.authconstants

object AppPasswordConstants {
    /** 应用密码最小长度（字符数） */
    const val MIN_PASSWORD_LENGTH: Int = 6

    /** SharedPreferences 文件名，用于持久化应用密码相关数据 */
    const val PREFS_NAME: String = "secure_db_prefs"

    /** SharedPreferences 中加密密码的 Base64 编码值 */
    const val KEY_APP_PASSWORD_WRAP: String = "db_phrase_app_wrap"

    /** SharedPreferences 中 Argon2id 密钥派生所用的盐值 Base64 编码 */
    const val KEY_APP_PASSWORD_SALT: String = "db_phrase_app_salt"

    /** 应用密码解密失败时的错误消息，用于与其他 IllegalArgumentException 区分 */
    internal const val ERROR_APP_PASSWORD_MISMATCH: String = "应用密码错误"

    /** 应用密码加密时 AES-GCM 初始向量的字节长度 */
    internal const val PASSPHRASE_IV_LENGTH: Int = 12

    /** AES-GCM 认证标签的比特长度，影响密文末尾 MAC 大小 */
    internal const val PASSPHRASE_GCM_TAG_BITS: Int = 128
}