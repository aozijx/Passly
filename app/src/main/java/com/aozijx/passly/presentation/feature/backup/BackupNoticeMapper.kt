package com.aozijx.passly.presentation.feature.backup

import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.feature.backup.internal.operation.BackupOperation

internal fun BackupEffect.toNoticeCode(): NoticeCode = when (this) {
    is BackupEffect.Succeeded -> when (operation) {
        BackupOperation.EXPORT -> NoticeCode.BACKUP_EXPORT_COMPLETED
        BackupOperation.IMPORT -> NoticeCode.BACKUP_IMPORT_COMPLETED
        BackupOperation.DIRECTORY_CHECK -> NoticeCode.BACKUP_DIRECTORY_CHECK_COMPLETED
    }

    is BackupEffect.Failed -> when (operation) {
        BackupOperation.EXPORT -> NoticeCode.BACKUP_EXPORT_FAILED
        BackupOperation.IMPORT -> NoticeCode.BACKUP_IMPORT_FAILED
        BackupOperation.DIRECTORY_CHECK -> NoticeCode.BACKUP_DIRECTORY_CHECK_FAILED
    }
}
