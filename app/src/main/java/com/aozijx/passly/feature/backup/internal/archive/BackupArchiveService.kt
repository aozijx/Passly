package com.aozijx.passly.feature.backup.internal.archive

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.feature.backup.internal.model.BackupExportRequest
import com.aozijx.passly.feature.backup.internal.model.BackupImportRequest

interface BackupArchiveService {
    suspend fun export(request: BackupExportRequest): AppResult<Unit>

    suspend fun import(request: BackupImportRequest): AppResult<Unit>

    suspend fun checkDirectoryWritable(uri: String): AppResult<Unit>
}
