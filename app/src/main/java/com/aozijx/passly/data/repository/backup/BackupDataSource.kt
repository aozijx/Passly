package com.aozijx.passly.data.repository.backup

import com.aozijx.passly.data.entity.VaultEntryEntity
import com.aozijx.passly.domain.model.backup.BackupImportMode

/**
 * 备份数据源契约，定义了备份时如何读取和写入原始数据库实体。
 * 仅限数据层内部使用。
 */
internal interface BackupDataSource {
    suspend fun readAllEntries(): List<VaultEntryEntity>
    suspend fun writeEntries(entries: List<VaultEntryEntity>, mode: BackupImportMode)
}