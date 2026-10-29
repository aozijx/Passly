package com.aozijx.passly.data.codec.entry

import com.aozijx.passly.data.codec.DatabaseRecordAad
import com.aozijx.passly.data.codec.entry.payload.SecretPayload
import com.aozijx.passly.data.codec.json.AppJson
import com.aozijx.passly.data.mapper.entry.EntrySecretMapper
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.sensitive.SecretFieldKeys
import com.aozijx.passly.core.crypto.FieldEncryptor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypts and decrypts the aggregated low-sensitivity secret bundle (`STRUCT_BUNDLE` row).
 * Field-level values (`PASSWORD`, card CVV, …) are never part of this payload.
 */
@Singleton
class SecretBundleCodec @Inject constructor(
    private val fieldEncryptor: FieldEncryptor
) {
    suspend fun decrypt(entityBlob: ByteArray, entryId: String, expectedType: EntryType): EntrySecret {
        val json = fieldEncryptor.decrypt(entityBlob, DatabaseRecordAad.secretBundle(entryId))
        val payload = AppJson.decodeFromString(SecretPayload.serializer(), json)
        return EntrySecretMapper.toDomain(payload, expectedType)
    }

    suspend fun encrypt(secret: EntrySecret, entryId: String, expectedType: EntryType): ByteArray {
        val payload = EntrySecretMapper.toPayload(secret, expectedType)
        val json = AppJson.encodeToString(SecretPayload.serializer(), payload)
        return fieldEncryptor.encrypt(json, DatabaseRecordAad.secretBundle(entryId))
    }

    companion object {
        const val FIELD_KEY = SecretFieldKeys.STRUCT_BUNDLE
    }
}
