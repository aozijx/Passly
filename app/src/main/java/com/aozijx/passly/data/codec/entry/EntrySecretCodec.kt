package com.aozijx.passly.data.codec.entry

import com.aozijx.passly.data.mapper.entry.EntrySecretMapper
import com.aozijx.passly.data.model.payload.secret.SecretPayload
import com.aozijx.passly.data.model.serializer.AppJson
import com.aozijx.passly.domain.model.entry.EntrySecret
import com.aozijx.passly.security.crypto.FieldEncryptor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntrySecretCodec @Inject constructor(
    private val fieldEncryptor: FieldEncryptor
) {
    private fun aad(entryId: String): ByteArray =
        "vault:$entryId:credential".toByteArray(Charsets.UTF_8)

    private fun aadOrNull(entryId: String): ByteArray? =
        if (entryId.isNotEmpty()) aad(entryId) else null

    suspend fun decrypt(entityBlob: ByteArray, entryId: String): EntrySecret {
        val json = fieldEncryptor.decrypt(entityBlob, aadOrNull(entryId))
        val payload = AppJson.decodeFromString(SecretPayload.serializer(), json)
        return EntrySecretMapper.toDomain(payload)
    }

    suspend fun encrypt(secret: EntrySecret, entryId: String): ByteArray {
        val payload = EntrySecretMapper.toPayload(secret)
        val json = AppJson.encodeToString(SecretPayload.serializer(), payload)
        return fieldEncryptor.encrypt(json, aad(entryId))
    }
}
