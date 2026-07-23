package com.aozijx.passly.data.codec.entry

import com.aozijx.passly.data.mapper.entry.EntrySummaryMapper
import com.aozijx.passly.data.model.payload.summary.SummaryPayload
import com.aozijx.passly.data.model.serializer.AppJson
import com.aozijx.passly.domain.model.entry.EntrySummary
import com.aozijx.passly.security.crypto.FieldEncryptor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntrySummaryCodec @Inject constructor(
    private val fieldEncryptor: FieldEncryptor
) {
    private fun aad(entryId: String): ByteArray =
        "vault:$entryId:metadata".toByteArray(Charsets.UTF_8)

    private fun aadOrNull(entryId: String): ByteArray? =
        if (entryId.isNotEmpty()) aad(entryId) else null

    suspend fun decrypt(entityBlob: ByteArray, entryId: String): EntrySummary {
        val json = fieldEncryptor.decrypt(entityBlob, aadOrNull(entryId))
        val payload = AppJson.decodeFromString(SummaryPayload.serializer(), json)
        return EntrySummaryMapper.toDomain(payload)
    }

    suspend fun encrypt(summary: EntrySummary, entryId: String): ByteArray {
        val payload = EntrySummaryMapper.toPayload(summary)
        val json = AppJson.encodeToString(SummaryPayload.serializer(), payload)
        return fieldEncryptor.encrypt(json, aad(entryId))
    }
}
