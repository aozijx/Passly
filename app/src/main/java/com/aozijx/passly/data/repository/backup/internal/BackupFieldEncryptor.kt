package com.aozijx.passly.data.repository.backup.internal

import com.aozijx.passly.data.entity.VaultEntryEntity
import com.aozijx.passly.security.crypto.FieldEncryptor

internal object BackupFieldEncryptor {

    fun toExportPayload(entity: VaultEntryEntity, iconPathForBackup: String?): VaultPayload {
        val json = FieldEncryptor.decrypt(entity.encryptedBlob)
        val payload = VaultPayload.fromJson(json)
        return if (iconPathForBackup != null) payload.copy(iconCustomPath = iconPathForBackup) else payload
    }

    fun toImportEntity(payload: VaultPayload): VaultEntryEntity {
        val encryptedBlob = FieldEncryptor.encrypt(payload.toJson())
        return VaultEntryEntity(
            entryType = payload.entryType,
            encryptedBlob = encryptedBlob,
            updatedAt = System.currentTimeMillis()
        )
    }
}