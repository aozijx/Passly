package com.aozijx.passly.security.crypto

import com.aozijx.passly.domain.auth.model.envelope.EnvelopeType
import com.aozijx.passly.domain.auth.model.envelope.KdfAlgorithm
import com.aozijx.passly.domain.auth.model.envelope.KeyEnvelope
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal object EnvelopeCrypto {
    private const val DEK_LENGTH = 32
    private const val IV_LENGTH = 12
    private const val GCM_TAG_BITS = 128
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private val random = SecureRandom()

    fun generateDek(): ByteArray = ByteArray(DEK_LENGTH).also(random::nextBytes)

    fun wrapWithKey(
        type: EnvelopeType,
        dek: ByteArray,
        wrappingKey: SecretKeySpec,
        salt: ByteArray,
        algorithm: KdfAlgorithm
    ): KeyEnvelope {
        val iv = ByteArray(IV_LENGTH).also(random::nextBytes)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        return KeyEnvelope(
            type = type,
            ciphertext = cipher.doFinal(dek),
            iv = iv,
            salt = salt,
            algorithm = algorithm,
            version = KeyEnvelope.VERSION_CURRENT
        )
    }

    fun wrapWithCipher(
        type: EnvelopeType,
        dek: ByteArray,
        cipher: Cipher
    ): KeyEnvelope = KeyEnvelope(
        type = type,
        ciphertext = cipher.doFinal(dek),
        iv = cipher.iv,
        salt = ByteArray(0),
        algorithm = KdfAlgorithm.NONE,
        version = KeyEnvelope.VERSION_CURRENT
    )

    fun unwrap(envelope: KeyEnvelope, cipher: Cipher): ByteArray =
        cipher.doFinal(envelope.ciphertext)

    fun verify(envelope: KeyEnvelope, key: SecretKeySpec): Boolean =
        try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, envelope.iv))
            cipher.doFinal(envelope.ciphertext)
            true
        } catch (_: Exception) {
            false
        }
}
