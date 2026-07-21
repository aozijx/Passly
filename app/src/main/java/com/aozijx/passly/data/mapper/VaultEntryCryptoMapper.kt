package com.aozijx.passly.data.mapper

import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.data.mapper.assembler.VaultEntryAssembler
import com.aozijx.passly.data.model.entity.VaultCredentialEntity
import com.aozijx.passly.data.model.entity.VaultMetadataEntity
import com.aozijx.passly.data.model.serializer.AppJson
import com.aozijx.passly.domain.model.entry.VaultCredential
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.entry.VaultMetadata
import com.aozijx.passly.security.crypto.FieldEncryptor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultEntryCryptoMapper @Inject constructor(
    private val fieldEncryptor: FieldEncryptor
) {

    private fun aad(uuid: String, column: String): ByteArray =
        "vault:$uuid:$column".toByteArray(Charsets.UTF_8)

    private fun aadOrNull(uuid: String, column: String): ByteArray? =
        if (uuid.isNotEmpty()) aad(uuid, column) else null

    suspend fun decryptMetadata(entity: VaultMetadataEntity): VaultMetadata {
        val json = fieldEncryptor.decrypt(entity.metadataBlob, aadOrNull(entity.entryId, "metadata"))
        return AppJson.decodeFromString(VaultMetadata.serializer(), json)
    }

    suspend fun decryptCredential(entity: VaultCredentialEntity): VaultCredential {
        val json = fieldEncryptor.decrypt(entity.credentialBlob, aadOrNull(entity.entryId, "credential"))
        return AppJson.decodeFromString(VaultCredential.serializer(), json)
    }

    suspend fun encryptMetadata(meta: VaultMetadata, uuid: String): ByteArray {
        val json = AppJson.encodeToString(VaultMetadata.serializer(), meta)
        return fieldEncryptor.encrypt(json, aad(uuid, "metadata"))
    }

    suspend fun encryptCredential(cred: VaultCredential, uuid: String): ByteArray {
        val json = AppJson.encodeToString(VaultCredential.serializer(), cred)
        return fieldEncryptor.encrypt(json, aad(uuid, "credential"))
    }

    suspend fun assembleEntry(
        metaEntity: VaultMetadataEntity,
        credEntity: VaultCredentialEntity?
    ): VaultEntry? {
        return try {
            val meta = decryptMetadata(metaEntity)
            val cred = credEntity?.let { decryptCredential(it) }
            VaultEntryAssembler.assembleFromDatabase(metaEntity, meta, cred)
        } catch (e: Exception) {
            AppLog.w("VaultRepo", "Skipping corrupt entry ${metaEntity.entryId}: ${e.message}")
            null
        }
    }
}