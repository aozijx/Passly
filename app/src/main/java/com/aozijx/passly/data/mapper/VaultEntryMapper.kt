package com.aozijx.passly.data.mapper

import com.aozijx.passly.data.entity.VaultEntryEntity
import com.aozijx.passly.data.entity.VaultHistoryEntity
import com.aozijx.passly.data.local.DatabaseConfig
import com.aozijx.passly.data.repository.backup.internal.VaultPayload
import com.aozijx.passly.data.repository.backup.internal.toVaultEntry
import com.aozijx.passly.data.repository.backup.internal.toVaultPayload
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.model.VaultHistory
import com.aozijx.passly.security.crypto.FieldEncryptor

/** AAD 绑定格式: table:uuid:column，确保密文黏性绑定到特定数据库单元格 */
private fun aad(table: String, uuid: String, column: String): ByteArray =
    "${table}:${uuid}:${column}".toByteArray(Charsets.UTF_8)

private fun aadOrNull(table: String, uuid: String, column: String): ByteArray? =
    if (uuid.isNotEmpty()) aad(table, uuid, column) else null

fun VaultEntryEntity.toDomain(fieldEncryptor: FieldEncryptor): VaultEntry {
    val json = fieldEncryptor.decrypt(
        encryptedBlob,
        aadOrNull(DatabaseConfig.TABLE_ENTRIES, uuid, "encryptedBlob")
    )
    val p = VaultPayload.fromJson(json)
    return p.toVaultEntry(id = id, uuid = uuid, updatedAt = updatedAt)
}

fun VaultEntry.toEntity(fieldEncryptor: FieldEncryptor): VaultEntryEntity {
    val payload = toVaultPayload()
    val entryUuid = uuid ?: java.util.UUID.randomUUID().toString()
    return VaultEntryEntity(
        id = id,
        entryType = entryType,
        uuid = entryUuid,
        encryptedBlob = fieldEncryptor.encrypt(
            payload.toJson(),
            aad(DatabaseConfig.TABLE_ENTRIES, entryUuid, "encryptedBlob")
        ),
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
