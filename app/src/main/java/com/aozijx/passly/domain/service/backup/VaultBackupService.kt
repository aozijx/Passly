package com.aozijx.passly.domain.service.backup

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.backup.BackupExportRequest
import com.aozijx.passly.domain.model.backup.BackupImportRequest

interface VaultBackupService {
    suspend fun export(request: BackupExportRequest): AppResult<Unit>

    suspend fun import(request: BackupImportRequest): AppResult<Unit>

    suspend fun checkDirectoryWritable(uri: String): AppResult<Unit>
}
