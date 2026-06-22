package com.aozijx.passly.domain.usecase.backup

import android.net.Uri
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.BackupImportMode
import com.aozijx.passly.domain.repository.backup.BackupRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupUseCases @Inject constructor(private val repository: BackupRepository) {

    suspend fun exportBackup(
        uri: Uri, password: CharArray, includeImages: Boolean
    ): AppResult<Unit> = repository.exportEncryptedBackup(uri, password, includeImages)

    suspend fun importBackup(
        uri: Uri, password: CharArray, mode: BackupImportMode
    ): AppResult<Unit> = repository.importBackup(uri, password, mode)

    suspend fun exportPlainBackup(uri: Uri): AppResult<Unit> =
        repository.exportPlainBackup(uri)

    suspend fun exportEmergencyBackup(): AppResult<File> =
        repository.exportEmergencyBackup()

    suspend fun testDirectoryWritePermission(directoryUri: String): AppResult<Unit> =
        repository.testDirectoryWritePermission(directoryUri)
}