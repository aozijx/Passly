package com.aozijx.passly.data.codec.revision

import com.aozijx.passly.data.codec.DatabaseRecordAad
import com.aozijx.passly.data.mapper.entry.EntrySecretMapper
import com.aozijx.passly.data.mapper.entry.EntryProfileMapper
import com.aozijx.passly.data.codec.entry.payload.SecretPayload
import com.aozijx.passly.data.codec.entry.payload.SummaryPayload
import com.aozijx.passly.data.codec.json.AppJson
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
import com.aozijx.passly.core.crypto.FieldEncryptor
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

data class EntryContentSnapshot(
    val summaryJson: ByteArray,
    val secretJson: ByteArray,
    val relationsJson: ByteArray,
) {
    fun toBytes(): ByteArray = ByteArrayOutputStream().also { output ->
        DataOutputStream(output).use { data ->
            listOf(summaryJson, secretJson, relationsJson).forEach { component ->
                data.writeInt(component.size)
                data.write(component)
            }
        }
    }.toByteArray()

    companion object {
        fun fromBytes(bytes: ByteArray): EntryContentSnapshot {
            val input = DataInputStream(ByteArrayInputStream(bytes))
            fun readComponent(name: String): ByteArray {
                val length = input.readInt()
                require(length in 0..MAX_COMPONENT_BYTES && length <= input.available()) {
                    "Invalid entry content $name length"
                }
                return ByteArray(length).also(input::readFully)
            }
            val summary = readComponent("summary")
            val secret = readComponent("secret")
            val relations = readComponent("relations")
            require(input.available() == 0) { "Trailing entry content snapshot bytes" }
            return EntryContentSnapshot(summary, secret, relations)
        }

        private const val MAX_COMPONENT_BYTES = 4 * 1024 * 1024
    }

    override fun equals(other: Any?): Boolean = other is EntryContentSnapshot &&
        summaryJson.contentEquals(other.summaryJson) && secretJson.contentEquals(other.secretJson) &&
        relationsJson.contentEquals(other.relationsJson)

    override fun hashCode(): Int = 31 * (31 * summaryJson.contentHashCode() + secretJson.contentHashCode()) +
        relationsJson.contentHashCode()
}

@Serializable
private data class EntryRelationsSnapshot(val links: List<EntryLinkSnapshot>)

@Serializable
private data class EntryLinkSnapshot(
    val id: String,
    val sourceEntryId: String,
    val targetEntryId: String,
    val relationType: String,
    val createdAt: Long,
    val updatedAt: Long,
)

data class DecodedEntryContentSnapshot(
    val summary: EntryProfile,
    val secret: EntrySecret,
    val links: List<EntryLink>,
)

@Singleton
class EntryContentSnapshotCodec @Inject constructor(
    private val fieldEncryptor: FieldEncryptor,
) {
    suspend fun encrypt(
        summary: EntryProfile,
        secret: EntrySecret,
        entryId: String,
        links: List<EntryLink>,
    ): ByteArray {
        val summaryJson = AppJson.encodeToString(
            SummaryPayload.serializer(), EntryProfileMapper.toPayload(summary)
        ).toByteArray()
        val secretJson = AppJson.encodeToString(
            SecretPayload.serializer(), EntrySecretMapper.toPayload(secret)
        ).toByteArray()
        val relationsJson = AppJson.encodeToString(
            EntryRelationsSnapshot.serializer(),
            EntryRelationsSnapshot(links.map(EntryLink::toSnapshot)),
        ).toByteArray()
        val compressed = gzip(EntryContentSnapshot(summaryJson, secretJson, relationsJson).toBytes())
        val encoded = FORMAT_PREFIX + Base64.getEncoder().encodeToString(compressed)
        return fieldEncryptor.encrypt(encoded, DatabaseRecordAad.revision(entryId))
    }

    suspend fun decrypt(cipher: ByteArray, entryId: String): DecodedEntryContentSnapshot {
        val encoded = fieldEncryptor.decrypt(cipher, DatabaseRecordAad.revision(entryId))
        require(encoded.startsWith(FORMAT_PREFIX)) { "Unsupported entry content snapshot format" }
        val snapshot = EntryContentSnapshot.fromBytes(
            gunzip(Base64.getDecoder().decode(encoded.removePrefix(FORMAT_PREFIX)))
        )
        val summary = AppJson.decodeFromString(
            SummaryPayload.serializer(), String(snapshot.summaryJson)
        )
        val secret = AppJson.decodeFromString(
            SecretPayload.serializer(), String(snapshot.secretJson)
        )
        val relations = AppJson.decodeFromString(
            EntryRelationsSnapshot.serializer(), String(snapshot.relationsJson)
        )
        return DecodedEntryContentSnapshot(
            summary = EntryProfileMapper.toDomain(summary),
            secret = EntrySecretMapper.toDomain(secret),
            links = relations.links.map(EntryLinkSnapshot::toDomain),
        )
    }

    private fun gzip(bytes: ByteArray): ByteArray = ByteArrayOutputStream().also { output ->
        GZIPOutputStream(output).use { it.write(bytes) }
    }.toByteArray()

    private fun gunzip(bytes: ByteArray): ByteArray = GZIPInputStream(ByteArrayInputStream(bytes)).use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            require(total <= MAX_SNAPSHOT_BYTES) { "Entry content snapshot exceeds size limit" }
            output.write(buffer, 0, read)
        }
        output.toByteArray()
    }

    private companion object {
        const val FORMAT_PREFIX = "content1:"
        const val MAX_SNAPSHOT_BYTES = 8 * 1024 * 1024
    }
}

private fun EntryLink.toSnapshot() = EntryLinkSnapshot(
    id.value, sourceEntryId.value, targetEntryId.value, relationType.name, createdAt, updatedAt
)

private fun EntryLinkSnapshot.toDomain(): EntryLink = EntryLink.create(
    id = EntryLinkId(id),
    sourceEntryId = EntryId(sourceEntryId),
    targetEntryId = EntryId(targetEntryId),
    relationType = EntryRelationType.valueOf(relationType),
    createdAt = createdAt,
    updatedAt = updatedAt,
)
