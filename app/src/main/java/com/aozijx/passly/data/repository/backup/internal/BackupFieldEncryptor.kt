package com.aozijx.passly.data.repository.backup.internal

import com.aozijx.passly.data.entity.VaultEntryEntity
import com.aozijx.passly.security.crypto.FieldEncryptor

internal object BackupFieldEncryptor {

    fun toExportPayload(
        entity: VaultEntryEntity,
        iconPathForBackup: String?,
        fieldEncryptor: FieldEncryptor
    ): VaultPayload {
        val json = fieldEncryptor.decrypt(entity.encryptedBlob)
        val payload = VaultPayload.fromJson(json)
        return if (iconPathForBackup != null) payload.copy(iconCustomPath = iconPathForBackup) else payload
    }

    fun toImportEntity(
        payload: VaultPayload,
        fieldEncryptor: FieldEncryptor
    ): VaultEntryEntity {
        return VaultEntryEntity(
            entryType = payload.entryType,
            encryptedBlob = fieldEncryptor.encrypt(payload.toJson()),
            updatedAt = System.currentTimeMillis()
        )
    }
}
