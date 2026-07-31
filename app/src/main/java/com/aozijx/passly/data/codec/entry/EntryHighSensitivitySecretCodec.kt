package com.aozijx.passly.data.codec.entry

import com.aozijx.passly.data.crypto.AadProvider
import com.aozijx.passly.data.mapper.entry.EntryHighSensitivitySecretMapper
import com.aozijx.passly.data.model.payload.secret.HighSensitivitySecretPayload
import com.aozijx.passly.data.model.serializer.AppJson
import com.aozijx.passly.domain.entry.model.EntryHighSensitivitySecret
import com.aozijx.passly.security.crypto.FieldEncryptor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntryHighSensitivitySecretCodec @Inject constructor(
    private val fieldEncryptor: FieldEncryptor
) {
    suspend fun decrypt(entityBlob: ByteArray, entryId: String): EntryHighSensitivitySecret {
        val json = fieldEncryptor.decrypt(entityBlob, AadProvider.highSensitivityCredential(entryId))
        val payload = AppJson.decodeFromString(HighSensitivitySecretPayload.serializer(), json)
        return EntryHighSensitivitySecretMapper.toDomain(payload)
    }

    suspend fun encrypt(secret: EntryHighSensitivitySecret, entryId: String): ByteArray {
        val payload = EntryHighSensitivitySecretMapper.toPayload(secret)
        val json = AppJson.encodeToString(HighSensitivitySecretPayload.serializer(), payload)
        return fieldEncryptor.encrypt(json, AadProvider.highSensitivityCredential(entryId))
    }
}
