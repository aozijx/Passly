package com.aozijx.passly.data.repository.backup.internal

import com.aozijx.passly.data.entity.VaultEntryEntity
import com.aozijx.passly.data.local.DatabaseConfig
import com.aozijx.passly.security.crypto.FieldEncryptor

internal object BackupFieldEncryptor {

    private fun aad(uuid: String, column: String): ByteArray =
        "${DatabaseConfig.TABLE_ENTRIES}:${uuid}:${column}".toByteArray(Charsets.UTF_8)

    private fun aadOrNull(uuid: String, column: String): ByteArray? =
        if (uuid.isNotEmpty()) aad(uuid, column) else null

    fun toExportPayload(
        entity: VaultEntryEntity,
        iconPathForBackup: String?,
        fieldEncryptor: FieldEncryptor
    ): VaultPayload {
        val json =
            fieldEncryptor.decrypt(entity.encryptedBlob, aadOrNull(entity.uuid, "encryptedBlob"))
        val payload = VaultPayload.fromJson(json)
        return if (iconPathForBackup != null) payload.copy(iconCustomPath = iconPathForBackup) else payload
    }

    fun toImportEntity(
        payload: VaultPayload,
        fieldEncryptor: FieldEncryptor
    ): VaultEntryEntity {
        return VaultEntryEntity(
            entryType = payload.entryType,
            uuid = payload.uuid,
            encryptedBlob = fieldEncryptor.encrypt(
                payload.toJson(),
                aad(payload.uuid, "encryptedBlob")
            ),
            updatedAt = System.currentTimeMillis()
        )
    }
}