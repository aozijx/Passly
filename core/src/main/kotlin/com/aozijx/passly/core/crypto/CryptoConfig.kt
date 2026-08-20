package com.aozijx.passly.core.crypto

object CryptoConfig {
    const val ALGORITHM: String = "AES/GCM/NoPadding"
    const val AES_KEY_ALGORITHM: String = "AES"
    const val IV_LENGTH: Int = 12
    const val GCM_TAG_BITS: Int = 128
    const val KEY_SIZE_BITS: Int = 256
    const val KEYSTORE_ALIAS_SUFFIX: String = "vault_db_hard_auth"
}
