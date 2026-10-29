package com.aozijx.passly.security.autofill.inbox

import com.aozijx.passly.core.crypto.CryptoConfig
import com.aozijx.passly.core.crypto.MemoryCleaner
import com.aozijx.passly.domain.access.model.AutofillInboxKeyMaterial
import com.aozijx.passly.domain.autofill.model.AutofillInboxDraftId
import com.aozijx.passly.domain.autofill.model.AutofillInboxLimits
import com.aozijx.passly.domain.autofill.model.AutofillInboxPendingDraft
import com.aozijx.passly.domain.autofill.model.AutofillInboxSource
import com.aozijx.passly.domain.autofill.model.AutofillSaveDraft
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.spec.MGF1ParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AutofillInboxRecordCodec @Inject constructor(
    private val keyMaterialCodec: AutofillInboxKeyMaterialCodec,
) {
    private val random = SecureRandom()

    fun createDraftId(): AutofillInboxDraftId =
        AutofillInboxDraftId.fromBytes(ByteArray(DRAFT_ID_BYTES).also(random::nextBytes))

    fun seal(
        material: AutofillInboxKeyMaterial,
        draftId: AutofillInboxDraftId,
        source: AutofillInboxSource,
        createdAtMs: Long,
        capturedAtMs: Long,
        draft: AutofillSaveDraft,
    ): ByteArray {
        require(createdAtMs >= 0) { "Inbox queue time cannot be negative" }
        val publicKey = keyMaterialCodec.validatedPublicKey(material)

        var plaintext: ByteArray? = null
        var contentKey: ByteArray? = null
        var wrappedContentKey: ByteArray? = null
        var ciphertextAndTag: ByteArray? = null
        try {
            plaintext = encodePayload(draftId, source, capturedAtMs, draft)
            require(plaintext.size <= MAX_PAYLOAD_BYTES) { "Inbox draft exceeds the record limit" }
            contentKey = ByteArray(CONTENT_KEY_BYTES).also(random::nextBytes)
            wrappedContentKey = rsaCipher(Cipher.ENCRYPT_MODE, publicKey).doFinal(contentKey)
            require(wrappedContentKey.size == RSA_CIPHERTEXT_BYTES) {
                "Unexpected Inbox RSA ciphertext size"
            }
            val nonce = ByteArray(CryptoConfig.IV_LENGTH).also(random::nextBytes)
            val aadPrefix = associatedDataPrefix(material.keyId, draftId, createdAtMs)
            val fingerprint = material.publicKeySha256
            val cipher = Cipher.getInstance(CryptoConfig.ALGORITHM)
            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(contentKey, CryptoConfig.AES_KEY_ALGORITHM),
                GCMParameterSpec(CryptoConfig.GCM_TAG_BITS, nonce),
            )
            cipher.updateAAD(aadPrefix + fingerprint)
            ciphertextAndTag = cipher.doFinal(plaintext)
            val ciphertextLength = ciphertextAndTag.size - GCM_TAG_BYTES
            val record = ByteBuffer.allocate(
                RECORD_AAD_HEADER_BYTES + U16_BYTES + wrappedContentKey.size +
                    CryptoConfig.IV_LENGTH + U32_BYTES + ciphertextAndTag.size,
            ).order(ByteOrder.BIG_ENDIAN)
                .put(aadPrefix)
                .putShort(wrappedContentKey.size.toShort())
                .put(wrappedContentKey)
                .put(nonce)
                .putInt(ciphertextLength)
                .put(ciphertextAndTag)
                .array()
            require(record.size <= AutofillInboxLimits.MAX_RECORD_BYTES) {
                "Inbox draft exceeds the record limit"
            }
            return record
        } finally {
            MemoryCleaner.wipeByteArray(plaintext)
            MemoryCleaner.wipeByteArray(contentKey)
            MemoryCleaner.wipeByteArray(wrappedContentKey)
            MemoryCleaner.wipeByteArray(ciphertextAndTag)
        }
    }

    fun open(
        material: AutofillInboxKeyMaterial,
        privateKey: PrivateKey,
        encoded: ByteArray,
    ): AutofillInboxPendingDraft {
        require(encoded.size in MIN_RECORD_BYTES..AutofillInboxLimits.MAX_RECORD_BYTES) {
            "Invalid Inbox record size"
        }
        require(privateKey.algorithm.equals(RSA_ALGORITHM, ignoreCase = true)) {
            "Inbox private key is not RSA"
        }
        keyMaterialCodec.validatedPublicKey(material)

        val header = decodeHeader(encoded)
        require(MessageDigest.isEqual(header.keyId, material.keyId)) { "Inbox record key ID mismatch" }

        var contentKey: ByteArray? = null
        var plaintext: ByteArray? = null
        try {
            val fingerprint = material.publicKeySha256
            val wrappedKey = header.wrappedContentKey
            contentKey = rsaCipher(Cipher.DECRYPT_MODE, privateKey).doFinal(wrappedKey)
            require(contentKey.size == CONTENT_KEY_BYTES) { "Invalid Inbox content key" }

            val cipher = Cipher.getInstance(CryptoConfig.ALGORITHM)
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(contentKey, CryptoConfig.AES_KEY_ALGORITHM),
                GCMParameterSpec(CryptoConfig.GCM_TAG_BITS, header.nonce),
            )
            cipher.updateAAD(header.aadPrefix + fingerprint)
            plaintext = cipher.doFinal(header.ciphertextAndTag)
            val pendingDraft = decodePayload(plaintext)
            require(pendingDraft.draftId == header.draftId) { "Inbox draft ID mismatch" }
            return pendingDraft.copy(queuedAtMs = header.createdAtMs)
        } finally {
            MemoryCleaner.wipeByteArray(contentKey)
            MemoryCleaner.wipeByteArray(plaintext)
            MemoryCleaner.wipeByteArray(header.wrappedContentKey)
            MemoryCleaner.wipeByteArray(header.nonce)
            MemoryCleaner.wipeByteArray(header.ciphertextAndTag)
        }
    }

    private fun decodeHeader(encoded: ByteArray): DecodedHeader {
        val buffer = ByteBuffer.wrap(encoded).order(ByteOrder.BIG_ENDIAN)
        val magic = ByteArray(RECORD_MAGIC.size).also(buffer::get)
        require(magic.contentEquals(RECORD_MAGIC)) { "Invalid Inbox record magic" }
        require(buffer.get() == RECORD_VERSION) { "Unsupported Inbox record version" }
        val keyId = ByteArray(KEY_ID_BYTES).also(buffer::get)
        val draftId = AutofillInboxDraftId.fromBytes(ByteArray(DRAFT_ID_BYTES).also(buffer::get))
        val createdAtMs = buffer.long
        require(createdAtMs >= 0) { "Invalid Inbox queue time" }
        val aadPrefixLength = buffer.position()
        val aadPrefix = encoded.copyOfRange(0, aadPrefixLength)

        val wrappedLength = buffer.short.toInt() and 0xffff
        require(wrappedLength == RSA_CIPHERTEXT_BYTES) { "Invalid wrapped Inbox content key length" }
        require(buffer.remaining() >= wrappedLength + CryptoConfig.IV_LENGTH + U32_BYTES + GCM_TAG_BYTES) {
            "Truncated Inbox record"
        }
        val wrappedContentKey = ByteArray(wrappedLength).also(buffer::get)
        val nonce = ByteArray(CryptoConfig.IV_LENGTH).also(buffer::get)
        val ciphertextLength = buffer.int
        require(ciphertextLength in 1..MAX_PAYLOAD_BYTES) { "Invalid Inbox ciphertext length" }
        require(buffer.remaining() == ciphertextLength + GCM_TAG_BYTES) {
            "Invalid Inbox record length"
        }
        return DecodedHeader(
            keyId = keyId,
            draftId = draftId,
            createdAtMs = createdAtMs,
            aadPrefix = aadPrefix,
            wrappedContentKey = wrappedContentKey,
            nonce = nonce,
            ciphertextAndTag = ByteArray(buffer.remaining()).also(buffer::get),
        )
    }

    private fun encodePayload(
        draftId: AutofillInboxDraftId,
        source: AutofillInboxSource,
        capturedAtMs: Long,
        draft: AutofillSaveDraft,
    ): ByteArray {
        require(capturedAtMs >= 0) { "Inbox capture time cannot be negative" }
        require(draft.packageName.isNotBlank()) { "Inbox package name is required" }
        require(draft.password.isNotBlank()) { "Inbox password is required" }

        val output = WipingByteArrayOutputStream()
        try {
            DataOutputStream(output).use { data ->
                data.write(PAYLOAD_MAGIC)
                data.writeByte(PAYLOAD_VERSION.toInt())
                data.write(draftId.toByteArray())
                data.writeByte(source.wireKey)
                data.writeLong(capturedAtMs)
                data.writeRequiredString(draft.packageName, MAX_PACKAGE_BYTES)
                data.writeNullableString(draft.webDomain, MAX_DOMAIN_BYTES)
                data.writeNullableString(draft.pageTitle, MAX_PAGE_TITLE_BYTES)
                data.writeRequiredString(draft.username, MAX_USERNAME_BYTES)
                data.writeRequiredString(draft.password, MAX_PASSWORD_BYTES)
            }
            return output.toByteArray()
        } finally {
            output.wipe()
        }
    }

    private fun decodePayload(plaintext: ByteArray): AutofillInboxPendingDraft = try {
        DataInputStream(ByteArrayInputStream(plaintext)).use { data ->
            val magic = ByteArray(PAYLOAD_MAGIC.size).also(data::readFully)
            require(magic.contentEquals(PAYLOAD_MAGIC)) { "Invalid Inbox payload magic" }
            require(data.readUnsignedByte().toByte() == PAYLOAD_VERSION) {
                "Unsupported Inbox payload version"
            }
            val draftId = AutofillInboxDraftId.fromBytes(ByteArray(DRAFT_ID_BYTES).also(data::readFully))
            val sourceKey = data.readUnsignedByte()
            val source = AutofillInboxSource.fromWireKey(sourceKey)
                ?: throw IllegalArgumentException("Unknown Inbox source")
            val capturedAtMs = data.readLong()
            require(capturedAtMs >= 0) { "Invalid Inbox capture time" }
            val draft = AutofillSaveDraft(
                packageName = data.readRequiredString(MAX_PACKAGE_BYTES),
                webDomain = data.readNullableString(MAX_DOMAIN_BYTES),
                pageTitle = data.readNullableString(MAX_PAGE_TITLE_BYTES),
                username = data.readRequiredString(MAX_USERNAME_BYTES),
                password = data.readRequiredString(MAX_PASSWORD_BYTES),
            )
            require(draft.packageName.isNotBlank()) { "Inbox package name is required" }
            require(draft.password.isNotBlank()) { "Inbox password is required" }
            require(data.available() == 0) { "Unexpected trailing Inbox payload bytes" }
            AutofillInboxPendingDraft(
                draftId = draftId,
                source = source,
                queuedAtMs = 0,
                capturedAtMs = capturedAtMs,
                draft = draft,
            )
        }
    } catch (truncated: EOFException) {
        throw IllegalArgumentException("Truncated Inbox payload", truncated)
    }

    private fun DataOutputStream.writeRequiredString(value: String, maxBytes: Int) {
        val bytes = encodeUtf8(value, maxBytes)
        try {
            writeInt(bytes.size)
            write(bytes)
        } finally {
            MemoryCleaner.wipeByteArray(bytes)
        }
    }

    private fun DataOutputStream.writeNullableString(value: String?, maxBytes: Int) {
        if (value == null) {
            writeInt(NULL_STRING_LENGTH)
        } else {
            writeRequiredString(value, maxBytes)
        }
    }

    private fun DataInputStream.readRequiredString(maxBytes: Int): String =
        readString(maxBytes, nullable = false) ?: error("Required Inbox field is missing")

    private fun DataInputStream.readNullableString(maxBytes: Int): String? =
        readString(maxBytes, nullable = true)

    private fun DataInputStream.readString(maxBytes: Int, nullable: Boolean): String? {
        val length = readInt()
        if (nullable && length == NULL_STRING_LENGTH) return null
        require(length in 0..maxBytes) { "Invalid Inbox string length" }
        require(available() >= length) { "Truncated Inbox string" }
        val bytes = ByteArray(length).also(::readFully)
        return try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } finally {
            MemoryCleaner.wipeByteArray(bytes)
        }
    }

    private fun encodeUtf8(value: String, maxBytes: Int): ByteArray {
        require(value.length <= maxBytes) { "Inbox text field exceeds its limit" }
        return value.toByteArray(StandardCharsets.UTF_8).also {
            require(it.size <= maxBytes) { "Inbox text field exceeds its limit" }
        }
    }

    private fun rsaCipher(mode: Int, key: java.security.Key): Cipher =
        Cipher.getInstance(RSA_OAEP_TRANSFORMATION).apply {
            init(
                mode,
                key,
                OAEPParameterSpec(
                    SHA_256,
                    MGF1,
                    MGF1ParameterSpec.SHA256,
                    PSource.PSpecified.DEFAULT,
                ),
            )
        }

    private fun associatedDataPrefix(
        keyId: ByteArray,
        draftId: AutofillInboxDraftId,
        createdAtMs: Long,
    ): ByteArray = ByteBuffer.allocate(RECORD_AAD_HEADER_BYTES)
        .order(ByteOrder.BIG_ENDIAN)
        .put(RECORD_MAGIC)
        .put(RECORD_VERSION)
        .put(keyId)
        .put(draftId.toByteArray())
        .putLong(createdAtMs)
        .array()

    private data class DecodedHeader(
        val keyId: ByteArray,
        val draftId: AutofillInboxDraftId,
        val createdAtMs: Long,
        val aadPrefix: ByteArray,
        val wrappedContentKey: ByteArray,
        val nonce: ByteArray,
        val ciphertextAndTag: ByteArray,
    )

    private class WipingByteArrayOutputStream : ByteArrayOutputStream() {
        fun wipe() {
            MemoryCleaner.wipeByteArray(buf)
            reset()
        }
    }

    private companion object {
        val RECORD_MAGIC = "PASSLYINBOX".toByteArray(StandardCharsets.US_ASCII)
        val PAYLOAD_MAGIC = "PASSLYDRAFT".toByteArray(StandardCharsets.US_ASCII)
        const val RECORD_VERSION: Byte = 1
        const val PAYLOAD_VERSION: Byte = 1
        const val RSA_ALGORITHM = "RSA"
        const val RSA_OAEP_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
        const val SHA_256 = "SHA-256"
        const val MGF1 = "MGF1"
        const val U16_BYTES = 2
        const val U32_BYTES = 4
        const val KEY_ID_BYTES = 16
        const val DRAFT_ID_BYTES = 16
        const val RSA_CIPHERTEXT_BYTES = 384
        const val CONTENT_KEY_BYTES = 32
        const val GCM_TAG_BYTES = 16
        const val RECORD_AAD_HEADER_BYTES = 11 + 1 + KEY_ID_BYTES + DRAFT_ID_BYTES + 8
        const val MIN_RECORD_BYTES = RECORD_AAD_HEADER_BYTES + U16_BYTES + RSA_CIPHERTEXT_BYTES +
            CryptoConfig.IV_LENGTH + U32_BYTES + GCM_TAG_BYTES + 1
        const val MAX_PAYLOAD_BYTES = AutofillInboxLimits.MAX_RECORD_BYTES - 512
        const val MAX_PACKAGE_BYTES = 512
        const val MAX_DOMAIN_BYTES = 4 * 1024
        const val MAX_PAGE_TITLE_BYTES = 8 * 1024
        const val MAX_USERNAME_BYTES = 8 * 1024
        const val MAX_PASSWORD_BYTES = 32 * 1024
        const val NULL_STRING_LENGTH = -1
    }
}
