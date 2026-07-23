package com.aozijx.passly.data.codec.snapshot

import com.aozijx.passly.data.mapper.entry.EntrySecretMapper
import com.aozijx.passly.data.mapper.entry.EntrySummaryMapper
import com.aozijx.passly.data.model.payload.secret.SecretPayload
import com.aozijx.passly.data.model.payload.summary.SummaryPayload
import com.aozijx.passly.data.model.serializer.AppJson
import com.aozijx.passly.domain.model.entry.EntrySecret
import com.aozijx.passly.domain.model.entry.EntrySummary
import com.aozijx.passly.security.crypto.FieldEncryptor
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class SnapshotPayload(
    val summaryJson: ByteArray,
    val secretJson: ByteArray
) {
    fun toBytes(): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).use { dos ->
            dos.writeInt(summaryJson.size)
            dos.write(summaryJson)
            dos.writeInt(secretJson.size)
            dos.write(secretJson)
        }
        return out.toByteArray()
    }

    companion object {
        fun fromBytes(data: ByteArray): SnapshotPayload {
            val input = DataInputStream(ByteArrayInputStream(data))
            val summaryLen = input.readInt()
            val summaryBytes = ByteArray(summaryLen).also { input.readFully(it) }
            val secretLen = input.readInt()
            val secretBytes = ByteArray(secretLen).also { input.readFully(it) }
            return SnapshotPayload(summaryBytes, secretBytes)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SnapshotPayload) return false
        return summaryJson.contentEquals(other.summaryJson) &&
                secretJson.contentEquals(other.secretJson)
    }

    override fun hashCode(): Int {
        var result = summaryJson.contentHashCode()
        result = 31 * result + secretJson.contentHashCode()
        return result
    }

    override fun toString(): String =
        "SnapshotPayload(summary=${summaryJson.size}B, secret=${secretJson.size}B)"
}

@Singleton
class EntrySnapshotCodec @Inject constructor(
    private val fieldEncryptor: FieldEncryptor
) {
    private fun aad(entryId: String): ByteArray =
        "vault:$entryId:snapshot".toByteArray(Charsets.UTF_8)

    suspend fun encrypt(
        summary: EntrySummary,
        secret: EntrySecret,
        entryId: String
    ): ByteArray {
        val summaryPayload = EntrySummaryMapper.toPayload(summary)
        val secretPayload = EntrySecretMapper.toPayload(secret)
        val summaryJson = AppJson.encodeToString(SummaryPayload.serializer(), summaryPayload)
            .toByteArray(Charsets.UTF_8)
        val secretJson = AppJson.encodeToString(SecretPayload.serializer(), secretPayload)
            .toByteArray(Charsets.UTF_8)
        val payload = SnapshotPayload(summaryJson, secretJson)
        return fieldEncryptor.encrypt(
            payload.toBytes().decodeToString(),
            aad(entryId)
        )
    }

    suspend fun decrypt(
        blob: ByteArray,
        entryId: String
    ): Pair<EntrySummary, EntrySecret> {
        val json = fieldEncryptor.decrypt(blob, aad(entryId))
        val snapshotPayload = SnapshotPayload.fromBytes(json.toByteArray(Charsets.UTF_8))
        val summary = AppJson.decodeFromString(
            SummaryPayload.serializer(),
            String(snapshotPayload.summaryJson, Charsets.UTF_8)
        )
        val secret = AppJson.decodeFromString(
            SecretPayload.serializer(),
            String(snapshotPayload.secretJson, Charsets.UTF_8)
        )
        return EntrySummaryMapper.toDomain(summary) to EntrySecretMapper.toDomain(secret)
    }
}
