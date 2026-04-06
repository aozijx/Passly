package com.aozijx.passly.core.backup

import com.aozijx.passly.data.entity.VaultEntryEntity

interface BackupDataSource {
    suspend fun readAllEntries(): List<VaultEntryEntity>
    suspend fun writeEntries(entries: List<VaultEntryEntity>, mode: BackupImportMode)
}

enum class BackupImportMode {
    APPEND,
    OVERWRITE
}