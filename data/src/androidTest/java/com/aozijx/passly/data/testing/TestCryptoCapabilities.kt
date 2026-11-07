package com.aozijx.passly.data.testing

import com.aozijx.passly.core.crypto.FieldKeyProvider
import com.aozijx.passly.core.crypto.SensitiveFieldCryptor

internal class TestFieldKeyProvider(
    key: ByteArray,
) : FieldKeyProvider {
    private val key = key.clone()

    override fun copyKey(): ByteArray = key.clone()

    fun clear() {
        key.fill(0)
    }
}

/** Repository integration tests exercise storage behavior; Security tests own crypto verification. */
internal object TestSensitiveFieldCryptor : SensitiveFieldCryptor {
    override suspend fun encrypt(
        data: String,
        aad: ByteArray,
    ): ByteArray = data.toByteArray(Charsets.UTF_8)

    override suspend fun decrypt(
        encryptedData: ByteArray,
        aad: ByteArray,
    ): String = encryptedData.toString(Charsets.UTF_8)
}

internal object NoCallSensitiveFieldCryptor : SensitiveFieldCryptor {
    override suspend fun encrypt(
        data: String,
        aad: ByteArray,
    ): ByteArray = error("This operation must not access sensitive fields")

    override suspend fun decrypt(
        encryptedData: ByteArray,
        aad: ByteArray,
    ): String = error("This operation must not access sensitive fields")
}
