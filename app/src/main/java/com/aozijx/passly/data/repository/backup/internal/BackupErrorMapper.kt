package com.aozijx.passly.data.repository.backup.internal

import com.aozijx.passly.core.backup.BackupManager
import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.core.error.ErrorLayer
import com.aozijx.passly.core.error.ErrorTrace
import com.aozijx.passly.domain.model.core.BackupException

internal fun mapBackupException(e: Throwable): BackupException {
    if (e is BackupException) return e
    val raw = BackupManager.mapImportFailure(e as? Exception ?: Exception(e))
    return when {
        raw.message?.contains("密码错误") == true -> BackupException.PasswordIncorrect()
        raw.message?.contains("文件损坏") == true -> BackupException.FileCorrupted()
        else -> BackupException.Unknown(e)
    }
}

internal fun mapToAppError(operation: String, e: Throwable): AppError {
    if (e is AppError) return e
    val backupException = mapBackupException(e)
    return AppError.BackupFailed(
        message = backupException.message ?: "备份操作失败",
        errorTrace = ErrorTrace(originLayer = ErrorLayer.DATA, operation = operation),
        cause = backupException
    )
}