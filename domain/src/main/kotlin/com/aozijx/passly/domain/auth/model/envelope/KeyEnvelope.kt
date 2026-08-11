package com.aozijx.passly.domain.auth.model.envelope

@JvmInline
value class EnvelopeType(val value: String) {
    companion object {
        val BIOMETRIC = EnvelopeType("biometric")
        val DEVICE_CREDENTIAL = EnvelopeType("device_credential")
        val APP_PASSWORD = EnvelopeType("app_password")
        val RECOVERY = EnvelopeType("recovery")
        val PASSKEY = EnvelopeType("passkey")
        val YUBIKEY = EnvelopeType("yubikey")
        val ENTERPRISE = EnvelopeType("enterprise")
    }
}

@JvmInline
value class KdfAlgorithm(val value: String) {
    companion object {
        val NONE = KdfAlgorithm("none")
        val ARGON2ID = KdfAlgorithm("argon2id")
        val PBKDF2 = KdfAlgorithm("pbkdf2")
    }
}

data class KeyEnvelope(
    val type: EnvelopeType,
    val ciphertext: ByteArray,
    val iv: ByteArray,
    val salt: ByteArray,
    val algorithm: KdfAlgorithm,
    val version: Int = 1
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KeyEnvelope) return false
        return type == other.type &&
                ciphertext.contentEquals(other.ciphertext) &&
                iv.contentEquals(other.iv) &&
                salt.contentEquals(other.salt) &&
                algorithm == other.algorithm &&
                version == other.version
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + ciphertext.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + algorithm.hashCode()
        result = 31 * result + version
        return result
    }

    companion object {
        const val VERSION_CURRENT = 1

        fun destroy(envelope: KeyEnvelope) {
            envelope.ciphertext.fill(0)
            envelope.iv.fill(0)
            envelope.salt.fill(0)
        }
    }
}
