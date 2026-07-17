package com.aozijx.passly.domain.model.envelope

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