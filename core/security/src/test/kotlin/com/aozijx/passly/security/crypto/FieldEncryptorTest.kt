package com.aozijx.passly.security.crypto

import com.aozijx.passly.core.error.boundary.CryptoException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class FieldEncryptorTest {

    @Test
    fun encrypt_usesFreshNonce_andBindsAad() {
        val keyManager = SessionKeyManager().apply {
            deriveAndSet(ByteArray(32) { it.toByte() })
        }
        val encryptor = FieldEncryptor(keyManager)
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
        keyManager.clearSessionKey()
    }

    @Test
    fun decrypt_rejectsPayloadShorterThanNonceAndTag() {
        val keyManager = SessionKeyManager().apply {
            deriveAndSet(ByteArray(32) { (it + 1).toByte() })
        }
        val encryptor = FieldEncryptor(keyManager)

        assertThrows(IllegalArgumentException::class.java) {
            encryptor.decrypt(ByteArray(CryptoConfig.IV_LENGTH + 15))
        }
        keyManager.clearSessionKey()
    }
}
