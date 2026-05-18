package com.aozijx.passly.core.crypto.cryptoconstants

object CryptoConstants {
    /** AES-GCM 加密算法标识 */
    const val ALGORITHM = "AES/GCM/NoPadding"

    /** AES 密钥算法名称 */
    const val AES_KEY_ALGORITHM = "AES"

    /** AES-GCM 初始向量的字节长度（96 位） */
    const val IV_LENGTH: Int = 12

    /** AES-GCM 认证标签的比特长度 */
    const val GCM_TAG_BITS: Int = 128

    /** AES 密钥长度（256 位） */
    const val KEY_SIZE_BITS: Int = 256

    /** SharedPreferences 文件名，用于持久化数据库口令 */
    const val PREFS_NAME: String = "secure_db_prefs"

    /** SharedPreferences 中数据库口令的 Base64 编码键名 */
    const val KEY_DB_PASSPHRASE: String = "db_phrase"

    /** Android KeyStore 别名的后缀模板，完整别名为 {packageName}.{aliasSuffix} */
    const val KEYSTORE_ALIAS_SUFFIX: String = "vault_db_hard_auth"

    /** HMAC-SHA256 派生 DEK 所用的标签 */
    const val DERIVE_LABEL = "passly-vault-field-key-v1"

    /** 随机生成的口令长度（字节） */
    const val GENERATED_PASSPHRASE_BYTES: Int = 32
}