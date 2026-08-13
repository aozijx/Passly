package com.aozijx.passly.data.codec.entry

import com.aozijx.passly.data.codec.DatabaseRecordAad
import com.aozijx.passly.data.mapper.entry.EntrySecretMapper
import com.aozijx.passly.data.codec.entry.payload.SecretPayload
import com.aozijx.passly.data.codec.json.AppJson
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.security.crypto.FieldEncryptor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntrySecretCodec @Inject constructor(
    private val fieldEncryptor: FieldEncryptor
) {
    suspend fun decrypt(entityBlob: ByteArray, entryId: String): EntrySecret {
        val json = fieldEncryptor.decrypt(entityBlob, DatabaseRecordAad.entrySecret(entryId))
        val payload = AppJson.decodeFromString(SecretPayload.serializer(), json)
        return EntrySecretMapper.toDomain(payload)
    }

    suspend fun encrypt(secret: EntrySecret, entryId: String): ByteArray {
        val payload = EntrySecretMapper.toPayload(secret)
        val json = AppJson.encodeToString(SecretPayload.serializer(), payload)
        return fieldEncryptor.encrypt(json, DatabaseRecordAad.entrySecret(entryId))
    }
}
