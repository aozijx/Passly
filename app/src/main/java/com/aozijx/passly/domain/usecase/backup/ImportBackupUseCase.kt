package com.aozijx.passly.domain.usecase.backup

import android.net.Uri
import com.aozijx.passly.core.backup.BackupImportMode
import com.aozijx.passly.domain.repository.backup.BackupRepository

class ImportBackupUseCase(private val repository: BackupRepository) {
    suspend operator fun invoke(
        uri: Uri,
        password: CharArray,
        mode: BackupImportMode
    ): Result<Unit> = repository.importBackup(uri, password, mode)
}