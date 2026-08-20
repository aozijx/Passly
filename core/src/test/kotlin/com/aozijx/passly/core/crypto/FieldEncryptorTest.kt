package com.aozijx.passly.core.crypto

import com.aozijx.passly.core.error.boundary.CryptoException
import com.aozijx.passly.security.dek.FieldKeyManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class FieldEncryptorTest {

    @Test
    fun encrypt_usesFreshNonce_andBindsAad() {
        val keyManager = FieldKeyManager().apply {
            deriveAndSet(ByteArray(32) { it.toByte() })
        }
        val encryptor = FieldEncryptor(keyManager, AesGcmCryptoEngine())
        val aad = "entry:attachment:content".toByteArray()

        val first = encryptor.encrypt("attachment", aad)
        val second = encryptor.encrypt("attachment", aad)

        assertFalse(first.contentEquals(second))
        assertFalse(
            first.copyOfRange(0, CryptoConfig.IV_LENGTH)
                .contentEquals(second.copyOfRange(0, CryptoConfig.IV_LENGTH))
        )
        assertEquals("attachment", encryptor.decrypt(first, aad))
        assertThrows(CryptoException.TagVerificationFailed::class.java) {
            encryptor.decrypt(first, "other-attachment".toByteArray())
        }
        keyManager.clear()
    }

    @Test
    fun decrypt_rejectsPayloadShorterThanNonceAndTag() {
        val keyManager = FieldKeyManager().apply {
            deriveAndSet(ByteArray(32) { (it + 1).toByte() })
        }
        val encryptor = FieldEncryptor(keyManager, AesGcmCryptoEngine())

        assertThrows(IllegalArgumentException::class.java) {
            encryptor.decrypt(ByteArray(CryptoConfig.IV_LENGTH + 15))
        }
        keyManager.clear()
    }
}
