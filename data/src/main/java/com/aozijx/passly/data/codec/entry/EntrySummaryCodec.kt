package com.aozijx.passly.data.codec.entry

import com.aozijx.passly.data.codec.DatabaseRecordAad
import com.aozijx.passly.data.mapper.entry.EntrySummaryMapper
import com.aozijx.passly.data.codec.entry.payload.SummaryPayload
import com.aozijx.passly.data.codec.json.AppJson
import com.aozijx.passly.domain.entry.model.EntrySummary
import com.aozijx.passly.security.crypto.FieldEncryptor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntrySummaryCodec @Inject constructor(
    private val fieldEncryptor: FieldEncryptor
) {
    suspend fun decrypt(entityBlob: ByteArray, entryId: String): EntrySummary {
        val json = fieldEncryptor.decrypt(entityBlob, DatabaseRecordAad.entrySummary(entryId))
        val payload = AppJson.decodeFromString(SummaryPayload.serializer(), json)
        return EntrySummaryMapper.toDomain(payload)
    }

    suspend fun encrypt(summary: EntrySummary, entryId: String): ByteArray {
        val payload = EntrySummaryMapper.toPayload(summary)
        val json = AppJson.encodeToString(SummaryPayload.serializer(), payload)
        return fieldEncryptor.encrypt(json, DatabaseRecordAad.entrySummary(entryId))
    }
}
