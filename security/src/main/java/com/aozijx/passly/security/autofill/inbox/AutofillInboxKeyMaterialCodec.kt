package com.aozijx.passly.security.autofill.inbox

import com.aozijx.passly.core.crypto.CryptoConfig
import com.aozijx.passly.core.crypto.MemoryCleaner
import com.aozijx.passly.domain.access.model.AutofillInboxKeyMaterial
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.interfaces.RSAPrivateCrtKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AutofillInboxKeyMaterialCodec @Inject constructor() {
    private val random = SecureRandom()

    fun generateAndWrap(dek: ByteArray): AutofillInboxKeyMaterial {
        require(dek.size == DEK_BYTES) { "Invalid Vault key size" }

        val keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM)
        keyPairGenerator.initialize(RSA_BITS, random)
        val keyPair = keyPairGenerator.generateKeyPair()
        val publicKey = keyPair.public.encoded
        val privateKey = keyPair.private.encoded
        val keyId = ByteArray(KEY_ID_BYTES).also(random::nextBytes)
        val fingerprint = MessageDigest.getInstance(SHA_256).digest(publicKey)
        var wrappingKey: ByteArray? = null
        try {
            wrappingKey = deriveWrappingKey(dek)
            val nonce = ByteArray(CryptoConfig.IV_LENGTH).also(random::nextBytes)
            val cipher = Cipher.getInstance(CryptoConfig.ALGORITHM)
            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(wrappingKey, CryptoConfig.AES_KEY_ALGORITHM),
                GCMParameterSpec(CryptoConfig.GCM_TAG_BITS, nonce),
            )
            cipher.updateAAD(associatedData(keyId, fingerprint))
            val ciphertextAndTag = cipher.doFinal(privateKey)
            try {
                val ciphertextLength = ciphertextAndTag.size - GCM_TAG_BYTES
                val envelope = ByteBuffer.allocate(ENVELOPE_HEADER_BYTES + ciphertextAndTag.size)
                    .order(ByteOrder.BIG_ENDIAN)
                    .put(MAGIC)
                    .put(ENVELOPE_VERSION)
                    .put(keyId)
                    .put(fingerprint)
                    .put(nonce)
                    .putInt(ciphertextLength)
                    .put(ciphertextAndTag)
                    .array()
                return AutofillInboxKeyMaterial(
                    formatVersion = AutofillInboxKeyMaterial.FORMAT_VERSION_CURRENT,
                    keyId = keyId,
                    publicKey = publicKey,
                    publicKeySha256 = fingerprint,
                    privateKeyEnvelope = envelope,
                )
            } finally {
                MemoryCleaner.wipeByteArray(ciphertextAndTag)
            }
        } finally {
            MemoryCleaner.wipeByteArray(privateKey)
            MemoryCleaner.wipeByteArray(wrappingKey)
        }
    }

    fun decodePublicKey(encoded: ByteArray): PublicKey = decodeAndValidatePublicKey(encoded)

    fun <T> withPrivateKey(
        material: AutofillInboxKeyMaterial,
        dek: ByteArray,
        block: (PrivateKey) -> T,
    ): T {
        require(dek.size == DEK_BYTES) { "Invalid Vault key size" }
        require(material.formatVersion == AutofillInboxKeyMaterial.FORMAT_VERSION_CURRENT) {
            "Unsupported Inbox key material version"
        }
        require(material.keyId.size == KEY_ID_BYTES) { "Invalid Inbox key identifier" }
        require(material.publicKeySha256.size == FINGERPRINT_BYTES) { "Invalid Inbox key fingerprint" }

        val publicKey = decodeAndValidatePublicKey(material.publicKey) as RSAPublicKey
        val fingerprint = MessageDigest.getInstance(SHA_256).digest(material.publicKey)
        require(MessageDigest.isEqual(fingerprint, material.publicKeySha256)) {
            "Inbox public key fingerprint mismatch"
        }

        val envelope = decodeEnvelope(material.privateKeyEnvelope, material.keyId, fingerprint)
        var wrappingKey: ByteArray? = null
        var encodedPrivateKey: ByteArray? = null
        try {
            wrappingKey = deriveWrappingKey(dek)
            val cipher = Cipher.getInstance(CryptoConfig.ALGORITHM)
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(wrappingKey, CryptoConfig.AES_KEY_ALGORITHM),
                GCMParameterSpec(CryptoConfig.GCM_TAG_BITS, envelope.nonce),
            )
            cipher.updateAAD(associatedData(material.keyId, fingerprint))
            encodedPrivateKey = cipher.doFinal(envelope.ciphertextAndTag)
            val privateKey = KeyFactory.getInstance(RSA_ALGORITHM)
                .generatePrivate(PKCS8EncodedKeySpec(encodedPrivateKey))
            validateKeyPair(privateKey, publicKey)
            return block(privateKey)
        } finally {
            MemoryCleaner.wipeByteArray(envelope.nonce)
            MemoryCleaner.wipeByteArray(envelope.ciphertextAndTag)
            MemoryCleaner.wipeByteArray(encodedPrivateKey)
            MemoryCleaner.wipeByteArray(wrappingKey)
        }
    }

    private fun decodeAndValidatePublicKey(encoded: ByteArray): PublicKey {
        require(encoded.size in 1..MAX_PUBLIC_KEY_BYTES) { "Invalid Inbox public key size" }
        val key = KeyFactory.getInstance(RSA_ALGORITHM)
            .generatePublic(X509EncodedKeySpec(encoded))
        val rsaKey = key as? RSAPublicKey
            ?: throw IllegalArgumentException("Inbox key is not RSA")
        require(rsaKey.modulus.bitLength() == RSA_BITS) { "Inbox key must be RSA-3072" }
        require(rsaKey.publicExponent == RSA_PUBLIC_EXPONENT) { "Invalid Inbox public exponent" }
        return rsaKey
    }

    private fun validateKeyPair(privateKey: PrivateKey, publicKey: RSAPublicKey) {
        val rsaPrivateKey = privateKey as? RSAPrivateCrtKey
            ?: throw IllegalArgumentException("Inbox private key is not RSA CRT")
        require(rsaPrivateKey.modulus.bitLength() == RSA_BITS) { "Inbox key must be RSA-3072" }
        require(rsaPrivateKey.modulus == publicKey.modulus) { "Inbox key modulus mismatch" }
        require(rsaPrivateKey.publicExponent == publicKey.publicExponent) {
            "Inbox key exponent mismatch"
        }
    }

    private fun decodeEnvelope(
        encoded: ByteArray,
        expectedKeyId: ByteArray,
        expectedFingerprint: ByteArray,
    ): DecodedEnvelope {
        require(encoded.size >= ENVELOPE_HEADER_BYTES + GCM_TAG_BYTES) {
            "Inbox private key envelope is truncated"
        }
        val buffer = ByteBuffer.wrap(encoded).order(ByteOrder.BIG_ENDIAN)
        val magic = ByteArray(MAGIC.size).also(buffer::get)
        require(magic.contentEquals(MAGIC)) { "Invalid Inbox private key envelope" }
        require(buffer.get() == ENVELOPE_VERSION) { "Unsupported Inbox private key envelope" }

        val keyId = ByteArray(KEY_ID_BYTES).also(buffer::get)
        val fingerprint = ByteArray(FINGERPRINT_BYTES).also(buffer::get)
        require(MessageDigest.isEqual(keyId, expectedKeyId)) { "Inbox key identifier mismatch" }
        require(MessageDigest.isEqual(fingerprint, expectedFingerprint)) {
            "Inbox private key fingerprint mismatch"
        }

        val nonce = ByteArray(CryptoConfig.IV_LENGTH).also(buffer::get)
        val ciphertextLength = buffer.int
        require(ciphertextLength in MIN_RSA_CIPHERTEXT_BYTES..MAX_RSA_CIPHERTEXT_BYTES) {
            "Invalid Inbox private key ciphertext length"
        }
        require(buffer.remaining() == ciphertextLength + GCM_TAG_BYTES) {
            "Invalid Inbox private key envelope length"
        }
        return DecodedEnvelope(nonce, ByteArray(buffer.remaining()).also(buffer::get))
    }

    private fun deriveWrappingKey(dek: ByteArray): ByteArray =
        Mac.getInstance(HMAC_SHA_256).run {
            init(SecretKeySpec(dek, HMAC_SHA_256))
            doFinal(WRAP_LABEL)
        }

    private fun associatedData(keyId: ByteArray, fingerprint: ByteArray): ByteArray =
        MAGIC + byteArrayOf(ENVELOPE_VERSION) + keyId + fingerprint

    private data class DecodedEnvelope(
        val nonce: ByteArray,
        val ciphertextAndTag: ByteArray,
    )

    private companion object {
        val MAGIC = "PASSLYINBOXKEY".toByteArray(Charsets.US_ASCII)
        val WRAP_LABEL = "passly-autofill-inbox-private-wrap-v1".toByteArray(Charsets.UTF_8)
        const val RSA_ALGORITHM = "RSA"
        const val RSA_BITS = 3072
        val RSA_PUBLIC_EXPONENT = BigInteger.valueOf(65537L)
        const val SHA_256 = "SHA-256"
        const val HMAC_SHA_256 = "HmacSHA256"
        const val DEK_BYTES = 32
        const val KEY_ID_BYTES = 16
        const val FINGERPRINT_BYTES = 32
        const val GCM_TAG_BYTES = 16
        const val ENVELOPE_HEADER_BYTES = 14 + 1 + KEY_ID_BYTES + FINGERPRINT_BYTES + 12 + 4
        const val MIN_RSA_CIPHERTEXT_BYTES = 1
        const val MAX_RSA_CIPHERTEXT_BYTES = 16 * 1024
        const val MAX_PUBLIC_KEY_BYTES = 2 * 1024
        const val ENVELOPE_VERSION: Byte = 1
    }
}
