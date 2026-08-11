package com.aozijx.passly.data.codec.revision

import com.aozijx.passly.data.crypto.AadProvider
import com.aozijx.passly.data.mapper.entry.EntrySecretMapper
import com.aozijx.passly.data.mapper.entry.EntrySummaryMapper
import com.aozijx.passly.data.model.payload.secret.SecretPayload
import com.aozijx.passly.data.model.payload.summary.SummaryPayload
import com.aozijx.passly.data.model.serializer.AppJson
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntrySummary
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.link.EntryLink
import com.aozijx.passly.domain.entry.model.link.EntryLinkId
import com.aozijx.passly.domain.entry.model.link.EntryRelationType
import com.aozijx.passly.security.crypto.FieldEncryptor
import kotlinx.serialization.Serializable
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class RevisionPayload(
    val summaryJson: ByteArray,
    val secretJson: ByteArray,
    val metadataJson: ByteArray,
) {
    fun toBytes(): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).use { dos ->
            dos.writeInt(summaryJson.size)
            dos.write(summaryJson)
            dos.writeInt(secretJson.size)
            dos.write(secretJson)
            dos.writeInt(metadataJson.size)
            dos.write(metadataJson)
        }
        return out.toByteArray()
    }

    companion object {
        fun fromBytes(data: ByteArray): RevisionPayload {
            val input = DataInputStream(ByteArrayInputStream(data))
            val summaryLen = input.readInt()
            require(summaryLen in 0..MAX_COMPONENT_BYTES) { "Invalid revision summary length" }
            require(summaryLen <= input.available()) { "Truncated revision summary" }
            val summaryBytes = ByteArray(summaryLen).also { input.readFully(it) }
            val secretLen = input.readInt()
            require(secretLen in 0..MAX_COMPONENT_BYTES) { "Invalid revision secret length" }
            require(secretLen <= input.available()) { "Truncated revision secret" }
            val secretBytes = ByteArray(secretLen).also { input.readFully(it) }
            val metadataLen = input.readInt()
            require(metadataLen in 0..MAX_COMPONENT_BYTES) { "Invalid revision metadata length" }
            require(metadataLen == input.available()) { "Invalid revision metadata payload" }
            val metadataBytes = ByteArray(metadataLen).also { input.readFully(it) }
            return RevisionPayload(summaryBytes, secretBytes, metadataBytes)
        }

        private const val MAX_COMPONENT_BYTES = 4 * 1024 * 1024
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RevisionPayload) return false
        return summaryJson.contentEquals(other.summaryJson) &&
                secretJson.contentEquals(other.secretJson) &&
                metadataJson.contentEquals(other.metadataJson)
    }

    override fun hashCode(): Int {
        var result = summaryJson.contentHashCode()
        result = 31 * result + secretJson.contentHashCode()
        result = 31 * result + metadataJson.contentHashCode()
        return result
    }

    override fun toString(): String =
        "RevisionPayload(summary=${summaryJson.size}B, secret=${secretJson.size}B, metadata=${metadataJson.size}B)"
}

@Serializable
private data class RevisionMetadataPayload(
    val links: List<RevisionLinkPayload>,
    val attachmentIds: List<String>,
)

@Serializable
private data class RevisionLinkPayload(
    val id: String,
    val sourceEntryId: String,
    val targetEntryId: String,
    val relationType: String,
    val createdAt: Long,
    val updatedAt: Long,
)

data class DecodedEntryRevisionSnapshot(
    val summary: EntrySummary,
    val secret: EntrySecret,
    val links: List<EntryLink>,
    val attachmentIds: List<String>,
)

@Singleton
class EntryRevisionCodec @Inject constructor(
    private val fieldEncryptor: FieldEncryptor
) {
    suspend fun encrypt(
        summary: EntrySummary,
        secret: EntrySecret,
        entryId: String,
        links: List<EntryLink>,
        attachmentIds: List<String>,
    ): ByteArray {
        val summaryPayload = EntrySummaryMapper.toPayload(summary)
        val secretPayload = EntrySecretMapper.toPayload(secret)
        val summaryJson = AppJson.encodeToString(SummaryPayload.serializer(), summaryPayload)
            .toByteArray(Charsets.UTF_8)
        val secretJson = AppJson.encodeToString(SecretPayload.serializer(), secretPayload)
            .toByteArray(Charsets.UTF_8)
        val metadataJson = AppJson.encodeToString(
            RevisionMetadataPayload.serializer(),
            RevisionMetadataPayload(
                links = links.map(EntryLink::toRevisionPayload),
                attachmentIds = attachmentIds.distinct().sorted(),
            ),
        ).toByteArray(Charsets.UTF_8)
        val payload = RevisionPayload(summaryJson, secretJson, metadataJson)
        val compressed = gzip(payload.toBytes())
        val encoded = FORMAT_V1_PREFIX + Base64.getEncoder().encodeToString(compressed)
        return fieldEncryptor.encrypt(
            encoded,
            AadProvider.revision(entryId)
        )
    }

    suspend fun decrypt(
        blob: ByteArray,
        entryId: String
    ): DecodedEntryRevisionSnapshot {
        val encoded = fieldEncryptor.decrypt(blob, AadProvider.revision(entryId))
        require(encoded.startsWith(FORMAT_V1_PREFIX)) {
            "Unsupported revision snapshot format"
        }
        val compressed = Base64.getDecoder().decode(encoded.removePrefix(FORMAT_V1_PREFIX))
        val payloadBytes = gunzip(compressed)
        val revisionPayload = RevisionPayload.fromBytes(payloadBytes)
        val summary = AppJson.decodeFromString(
            SummaryPayload.serializer(),
            String(revisionPayload.summaryJson, Charsets.UTF_8)
        )
        val secret = AppJson.decodeFromString(
            SecretPayload.serializer(),
            String(revisionPayload.secretJson, Charsets.UTF_8)
        )
        val metadata = AppJson.decodeFromString(
            RevisionMetadataPayload.serializer(),
            String(revisionPayload.metadataJson, Charsets.UTF_8),
        )
        return DecodedEntryRevisionSnapshot(
            summary = EntrySummaryMapper.toDomain(summary),
            secret = EntrySecretMapper.toDomain(secret),
            links = metadata.links.map(RevisionLinkPayload::toDomain),
            attachmentIds = metadata.attachmentIds,
        )
    }

    private fun gzip(data: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).use { it.write(data) }
        return output.toByteArray()
    }

    private fun gunzip(data: ByteArray): ByteArray =
        GZIPInputStream(ByteArrayInputStream(data)).use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                require(total <= MAX_REVISION_BYTES) { "Revision payload exceeds size limit" }
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        }

    private companion object {
        const val FORMAT_V1_PREFIX = "rev1:"
        const val MAX_REVISION_BYTES = 8 * 1024 * 1024
    }
}

private fun EntryLink.toRevisionPayload() = RevisionLinkPayload(
    id = id.value,
    sourceEntryId = sourceEntryId.value,
    targetEntryId = targetEntryId.value,
    relationType = relationType.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun RevisionLinkPayload.toDomain(): EntryLink = EntryLink.create(
    id = EntryLinkId(id),
    sourceEntryId = EntryId(sourceEntryId),
    targetEntryId = EntryId(targetEntryId),
    relationType = EntryRelationType.valueOf(relationType),
    createdAt = createdAt,
    updatedAt = updatedAt,
)
