package com.aozijx.passly.data.mapper

import com.aozijx.passly.data.entity.VaultEntryEntity
import com.aozijx.passly.data.entity.VaultHistoryEntity
import com.aozijx.passly.data.repository.backup.internal.VaultPayload
import com.aozijx.passly.data.repository.backup.internal.toVaultEntry
import com.aozijx.passly.data.repository.backup.internal.toVaultPayload
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.model.VaultHistory
import com.aozijx.passly.security.crypto.FieldEncryptor

fun VaultEntryEntity.toDomain(fieldEncryptor: FieldEncryptor): VaultEntry {
    val json = fieldEncryptor.decrypt(encryptedBlob)
    val p = VaultPayload.fromJson(json)
    return p.toVaultEntry(id = id, updatedAt = updatedAt)
}

fun VaultEntry.toEntity(fieldEncryptor: FieldEncryptor): VaultEntryEntity {
    val payload = toVaultPayload()
    return VaultEntryEntity(
        id = id,
        entryType = entryType,
        encryptedBlob = fieldEncryptor.encrypt(payload.toJson()),
        updatedAt = updatedAt ?: System.currentTimeMillis()
    )
}

fun VaultHistoryEntity.toDomain(): VaultHistory = VaultHistory(
    historyId = historyId,
    entryId = entryId,
    fieldName = fieldName,
    oldValue = oldValue,
    newValue = newValue,
    changeType = VaultHistory.HistoryType.entries.find { it.value == changeType }
        ?: VaultHistory.HistoryType.UPDATE,
    deviceName = deviceName,
    changedAt = changedAt
)

fun VaultHistory.toEntity(): VaultHistoryEntity = VaultHistoryEntity(
    historyId = historyId,
    entryId = entryId,
    fieldName = fieldName,
    oldValue = oldValue,
    newValue = newValue,
    changeType = changeType.value,
    deviceName = deviceName,
    changedAt = changedAt
)

fun List<VaultEntryEntity>.toDomainList(fieldEncryptor: FieldEncryptor): List<VaultEntry> =
    map { it.toDomain(fieldEncryptor) }

fun List<VaultHistoryEntity>.toDomainHistoryList(): List<VaultHistory> =
    map { it.toDomain() }
