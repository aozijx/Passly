package com.aozijx.passly.security.autofill.inbox

import com.aozijx.passly.domain.access.model.AutofillInboxKeyMaterial
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.Key
import java.security.interfaces.RSAPublicKey
import java.security.spec.MGF1ParameterSpec
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

class AutofillInboxKeyMaterialCodecTest {
    private val codec = AutofillInboxKeyMaterialCodec()
    private val dek = ByteArray(32) { it.toByte() }

    @Test
    fun generatedKeyMaterialRoundTripsAndEncryptsWithThePublishedKey() {
        val material = codec.generateAndWrap(dek)

        assertEquals(AutofillInboxKeyMaterial.FORMAT_VERSION_CURRENT, material.formatVersion)
        assertEquals(KEY_ID_BYTES, material.keyId.size)
        assertEquals(FINGERPRINT_BYTES, material.publicKeySha256.size)
        assertEquals(RSA_BITS, (codec.decodePublicKey(material.publicKey) as RSAPublicKey).modulus.bitLength())

        val plaintext = byteArrayOf(7, 8, 9)
        val cipher = rsaCipher(Cipher.ENCRYPT_MODE, codec.decodePublicKey(material.publicKey))
        val ciphertext = cipher.doFinal(plaintext)
        val decrypted = codec.withPrivateKey(material, dek) { privateKey ->
            rsaCipher(Cipher.DECRYPT_MODE, privateKey).doFinal(ciphertext)
        }

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun wrongDekCannotUnwrapThePrivateKey() {
        val material = codec.generateAndWrap(dek)

        assertThrows(Exception::class.java) {
            codec.withPrivateKey(material, ByteArray(32) { 0x5a }) { Unit }
        }
    }

    @Test
    fun inconsistentPublicKeyFingerprintIsRejectedBeforeUnwrap() {
        val material = codec.generateAndWrap(dek)
        val corrupted = material.copy(
            publicKeySha256 = material.publicKeySha256.clone().also {
                it[0] = (it[0].toInt() + 1).toByte()
            },
        )

        assertThrows(IllegalArgumentException::class.java) {
            codec.withPrivateKey(corrupted, dek) { Unit }
        }
    }

    @Test
    fun modifiedPrivateKeyEnvelopeFailsAuthentication() {
        val material = codec.generateAndWrap(dek)
        val corruptedEnvelope = material.privateKeyEnvelope.clone().also {
            it[it.lastIndex] = (it.last().toInt() xor 1).toByte()
        }

        assertThrows(AEADBadTagException::class.java) {
            codec.withPrivateKey(material.copy(privateKeyEnvelope = corruptedEnvelope), dek) { Unit }
        }
    }

    @Test
    fun unsupportedMaterialVersionIsRejected() {
        val material = codec.generateAndWrap(dek).copy(formatVersion = 2)

        assertThrows(IllegalArgumentException::class.java) {
            codec.withPrivateKey(material, dek) { Unit }
        }
    }

    @Test
    fun privateKeyEnvelopeUsesTheDocumentedHeaderAndLength() {
        val material = codec.generateAndWrap(dek)
        val envelope = ByteBuffer.wrap(material.privateKeyEnvelope).order(ByteOrder.BIG_ENDIAN)

        val magic = ByteArray(MAGIC.size).also(envelope::get)
        assertArrayEquals(MAGIC, magic)
        assertEquals(1, envelope.get().toInt())
        assertArrayEquals(material.keyId, ByteArray(KEY_ID_BYTES).also(envelope::get))
        assertArrayEquals(material.publicKeySha256, ByteArray(FINGERPRINT_BYTES).also(envelope::get))
        envelope.position(envelope.position() + NONCE_BYTES)
        val ciphertextLength = envelope.int

        assertTrue(ciphertextLength > 0)
        assertEquals(ciphertextLength + GCM_TAG_BYTES, envelope.remaining())
    }

    private fun rsaCipher(mode: Int, key: Key): Cipher =
        Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding").apply {
            init(
                mode,
                key,
                OAEPParameterSpec(
                    "SHA-256",
                    "MGF1",
                    MGF1ParameterSpec.SHA256,
                    PSource.PSpecified.DEFAULT,
                ),
            )
        }

    private companion object {
        const val KEY_ID_BYTES = 16
        const val FINGERPRINT_BYTES = 32
        const val RSA_BITS = 3072
        const val NONCE_BYTES = 12
        const val GCM_TAG_BYTES = 16
        val MAGIC = "PASSLYINBOXKEY".toByteArray(Charsets.US_ASCII)
    }
}
