package com.aozijx.passly.app.observability

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.aozijx.passly.core.observability.HashedIdentifier
import com.aozijx.passly.core.observability.IdentifierNamespace
import com.aozijx.passly.core.observability.privacy.IdentifierHasher
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.Base64
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

class AndroidKeystoreIdentifierHasher internal constructor(
    private val keyAlias: String = KEY_ALIAS,
) : IdentifierHasher {
    override fun hash(namespace: IdentifierNamespace, rawValue: String): HashedIdentifier? = try {
        val input = "$DOMAIN_SEPARATOR\u0000${namespace.wireName}\u0000$rawValue"
            .toByteArray(StandardCharsets.UTF_8)
        try {
            val mac = Mac.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256)
            mac.init(getOrCreateKey())
            val digest = mac.doFinal(input)
            try {
                val encoded = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(digest.copyOf(HASH_BYTES))
                HashedIdentifier("h1_$encoded")
            } finally {
                digest.fill(0)
            }
        } finally {
            input.fill(0)
        }
    } catch (_: Exception) {
        null
    }

    private fun getOrCreateKey(): SecretKey = synchronized(KEY_LOCK) {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey) ?: generateKey()
    }

    private fun generateKey(): SecretKey =
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_SIGN)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setKeySize(256)
                    .setUserAuthenticationRequired(false)
                    .build(),
            )
            generateKey()
        }

    private companion object {
        const val KEY_ALIAS = "com.aozijx.passly.observability.hmac.v1"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val DOMAIN_SEPARATOR = "passly-observability-v1"
        const val HASH_BYTES = 16
        val KEY_LOCK = Any()
    }
}
