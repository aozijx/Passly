package com.aozijx.passly.domain.backup.service

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.backup.model.BackupExportRequest
import com.aozijx.passly.domain.backup.model.BackupImportRequest

interface VaultBackupService {
    suspend fun export(request: BackupExportRequest): AppResult<Unit>

    suspend fun import(request: BackupImportRequest): AppResult<Unit>

    suspend fun checkDirectoryWritable(uri: String): AppResult<Unit>
}
