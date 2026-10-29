package com.aozijx.passly.domain.access.model

import java.security.MessageDigest

data class AutofillInboxKeyMaterial(
    val formatVersion: Int,
    val keyId: ByteArray,
    val publicKey: ByteArray,
    val publicKeySha256: ByteArray,
    val privateKeyEnvelope: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AutofillInboxKeyMaterial) return false
        return formatVersion == other.formatVersion &&
            MessageDigest.isEqual(keyId, other.keyId) &&
            MessageDigest.isEqual(publicKey, other.publicKey) &&
            MessageDigest.isEqual(publicKeySha256, other.publicKeySha256) &&
            MessageDigest.isEqual(privateKeyEnvelope, other.privateKeyEnvelope)
    }

    override fun hashCode(): Int {
        var result = formatVersion
        result = 31 * result + keyId.contentHashCode()
        result = 31 * result + publicKey.contentHashCode()
        result = 31 * result + publicKeySha256.contentHashCode()
        result = 31 * result + privateKeyEnvelope.contentHashCode()
        return result
    }

    companion object {
        const val FORMAT_VERSION_CURRENT = 1
    }
}
