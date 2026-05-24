package com.aozijx.passly.domain.usecase.backup

import android.net.Uri
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.BackupImportMode
import com.aozijx.passly.domain.repository.backup.BackupRepository

class ImportBackupUseCase(private val repository: BackupRepository) {
    suspend operator fun invoke(
        uri: Uri,
        password: CharArray,
        mode: BackupImportMode
    ): AppResult<Unit> = AppResult.runSuspendCatching("domain.backup.import") {
        repository.importBackup(uri, password, mode)
    }
}