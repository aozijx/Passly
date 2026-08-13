package com.aozijx.passly.security.search

import com.aozijx.passly.security.dek.DekManager
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 从 DEK 派生出 Blind Index 专用的 HMAC 密钥。
 *
 * SearchKey = HMAC-SHA256(DEK, "passly-search-key-v1")
 *
 * 与 [com.aozijx.passly.core.crypto.FieldEncryptor] 同层派生，
 * 但使用不同的 domain separation 标签，确保密钥用途隔离。
 */
@Singleton
class SearchKeyProvider @Inject constructor(
    private val dekManager: DekManager
) {
    suspend fun get(): ByteArray = dekManager.withDek { dek ->
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(dek, HMAC_ALGORITHM))
        mac.doFinal(DOMAIN_SEPARATION.toByteArray(Charsets.UTF_8))
    }

    companion object {
        private const val HMAC_ALGORITHM = "HmacSHA256"
        private const val DOMAIN_SEPARATION = "passly-search-key-v1"
    }
}
