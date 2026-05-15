package com.aozijx.passly.domain.usecase.backup

import android.net.Uri
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.repository.backup.BackupRepository

class ExportBackupUseCase(private val repository: BackupRepository) {
    suspend operator fun invoke(
        uri: Uri,
        password: CharArray,
        includeImages: Boolean
    ): AppResult<Unit> = AppResult.runSuspendCatching("domain.backup.exportEncrypted") {
        repository.exportEncryptedBackup(uri, password, includeImages)
    }
}