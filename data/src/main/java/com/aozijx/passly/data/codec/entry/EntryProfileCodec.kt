package com.aozijx.passly.data.codec.entry

import com.aozijx.passly.data.codec.DatabaseRecordAad
import com.aozijx.passly.data.mapper.entry.EntryProfileMapper
import com.aozijx.passly.data.codec.entry.payload.SummaryPayload
import com.aozijx.passly.data.codec.json.AppJson
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.core.crypto.FieldEncryptor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntryProfileCodec @Inject constructor(
    private val fieldEncryptor: FieldEncryptor
) {
    suspend fun decrypt(entityBlob: ByteArray, entryId: String): EntryProfile {
        val json = fieldEncryptor.decrypt(entityBlob, DatabaseRecordAad.entrySummary(entryId))
        val payload = AppJson.decodeFromString(SummaryPayload.serializer(), json)
        return EntryProfileMapper.toDomain(payload)
    }

    suspend fun encrypt(summary: EntryProfile, entryId: String): ByteArray {
        val payload = EntryProfileMapper.toPayload(summary)
        val json = AppJson.encodeToString(SummaryPayload.serializer(), payload)
        return fieldEncryptor.encrypt(json, DatabaseRecordAad.entrySummary(entryId))
    }
}
