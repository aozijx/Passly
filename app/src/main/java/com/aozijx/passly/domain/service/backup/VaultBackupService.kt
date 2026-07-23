package com.aozijx.passly.domain.service.backup

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.backup.ImportMode
import java.io.File

interface VaultBackupService {
    suspend fun exportEncryptedBackup(
        uri: String,
        password: CharArray,
        includeImages: Boolean
    ): AppResult<Unit>

    suspend fun exportPlainBackup(uri: String): AppResult<Unit>

    suspend fun exportEmergencyBackup(): AppResult<File>

    suspend fun importBackup(
        uri: String,
        password: CharArray,
        config: ImportMode
    ): AppResult<Unit>

    suspend fun importPlainBackup(
        uri: String,
        config: ImportMode
    ): AppResult<Unit>

    suspend fun checkDirectoryWritable(uri: String): AppResult<Unit>
}
