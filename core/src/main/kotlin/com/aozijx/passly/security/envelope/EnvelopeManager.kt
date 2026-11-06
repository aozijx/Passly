package com.aozijx.passly.security.envelope

import com.aozijx.passly.domain.access.model.AuthenticationKdfParameters
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.KdfAlgorithm
import com.aozijx.passly.domain.access.model.KeyEnvelope
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val DEK_LENGTH = 32
private const val IV_LENGTH = 12
private const val GCM_TAG_BITS = 128
private const val AUTHENTICATION_SALT_LENGTH = 16
private const val GCM_TAG_BYTES = GCM_TAG_BITS / Byte.SIZE_BITS
private const val ALGORITHM = "AES/GCM/NoPadding"
private const val AUTHENTICATION_CONTEXT_LABEL = "Passly.authentication-envelope.v1"

object EnvelopeManager {
    private val random = SecureRandom()

    fun generateDek(): ByteArray = ByteArray(DEK_LENGTH).also(random::nextBytes)

    fun wrapWithKey(
        type: EnvelopeType,
        dek: ByteArray,
        wrappingKey: SecretKeySpec,
        salt: ByteArray,
        algorithm: KdfAlgorithm,
        parameters: AuthenticationKdfParameters,
    ): KeyEnvelope {
        val iv = ByteArray(IV_LENGTH).also(random::nextBytes)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(authenticationAad(type, algorithm, salt, KeyEnvelope.VERSION_CURRENT, parameters))
        return KeyEnvelope(
            type = type,
            ciphertext = cipher.doFinal(dek),
            iv = iv,
            salt = salt,
            algorithm = algorithm,
            version = KeyEnvelope.VERSION_CURRENT,
        )
    }

    fun authenticationAad(
        envelope: KeyEnvelope,
        parameters: AuthenticationKdfParameters,
    ): ByteArray {
        require(envelope.iv.size == IV_LENGTH) { "Invalid authentication envelope IV" }
        require(envelope.ciphertext.size >= GCM_TAG_BYTES) { "Invalid authentication envelope ciphertext" }
        return authenticationAad(envelope.type, envelope.algorithm, envelope.salt, envelope.version, parameters)
    }

    private fun authenticationAad(
        type: EnvelopeType,
        algorithm: KdfAlgorithm,
        salt: ByteArray,
        version: Int,
        parameters: AuthenticationKdfParameters,
    ): ByteArray {
        require(type == EnvelopeType.APP_PASSWORD || type == EnvelopeType.RECOVERY) { "Invalid authentication envelope type" }
        require(algorithm == KdfAlgorithm.ARGON2ID) { "Invalid authentication envelope algorithm" }
        require(version == KeyEnvelope.VERSION_CURRENT) { "Invalid authentication envelope version" }
        require(salt.size == AUTHENTICATION_SALT_LENGTH) { "Invalid authentication envelope salt" }
        return ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                val label = AUTHENTICATION_CONTEXT_LABEL.toByteArray(StandardCharsets.UTF_8)
                val typeBytes = type.value.toByteArray(StandardCharsets.UTF_8)
                val algorithmBytes = algorithm.value.toByteArray(StandardCharsets.UTF_8)
                output.writeInt(label.size)
                output.write(label)
                output.writeInt(version)
                output.writeInt(typeBytes.size)
                output.write(typeBytes)
                output.writeInt(algorithmBytes.size)
                output.write(algorithmBytes)
                output.writeInt(salt.size)
                output.write(salt)
                output.writeInt(parameters.version)
                output.writeInt(parameters.memoryKiB)
                output.writeInt(parameters.iterations)
                output.writeInt(parameters.parallelism)
                output.writeInt(parameters.keyLengthBits)
            }
            bytes.toByteArray()
        }
    }

    fun wrapWithCipher(
        type: EnvelopeType,
        dek: ByteArray,
        cipher: Cipher,
    ): KeyEnvelope =
        KeyEnvelope(
            type = type,
            ciphertext = cipher.doFinal(dek),
            iv = cipher.iv,
            salt = ByteArray(0),
            algorithm = KdfAlgorithm.NONE,
            version = KeyEnvelope.VERSION_CURRENT,
        )

    fun unwrap(
        envelope: KeyEnvelope,
        cipher: Cipher,
    ): ByteArray = cipher.doFinal(envelope.ciphertext)
}
