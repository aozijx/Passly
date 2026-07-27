package com.aozijx.passly.domain.backup.failure

import com.aozijx.passly.domain.failure.AppFailure
import com.aozijx.passly.domain.failure.FailureCode
import com.aozijx.passly.domain.failure.FailureOrigin
import com.aozijx.passly.domain.failure.FailureSeverity
import com.aozijx.passly.domain.failure.RecoveryAction
import java.util.UUID

/**
 * 备份领域失败类型。
 */
sealed class BackupFailure(
    override val code: String,
    override val severity: FailureSeverity = FailureSeverity.ERROR,
    override val recoveryAction: RecoveryAction = RecoveryAction.NONE,
    override val correlationId: String = UUID.randomUUID().toString()
) : AppFailure {
    override val origin: FailureOrigin get() = FailureOrigin.DOMAIN

    /** 备份文件不存在 */
    data object FileNotFound : BackupFailure(
        code = FailureCode.BACKUP_FILE_NOT_FOUND,
        recoveryAction = RecoveryAction.NONE
    )

    /** 不支持的备份格式 */
    data object FormatUnsupported : BackupFailure(
        code = FailureCode.BACKUP_FORMAT_UNSUPPORTED,
        recoveryAction = RecoveryAction.NONE
    )

    /** 备份文件损坏 */
    data object Corrupted : BackupFailure(
        code = FailureCode.BACKUP_CORRUPTED,
        severity = FailureSeverity.ERROR,
        recoveryAction = RecoveryAction.NONE
    )

    /** 备份加密不匹配（密码/密钥错误） */
    data object EncryptionMismatch : BackupFailure(
        code = FailureCode.BACKUP_ENCRYPTION_MISMATCH,
        severity = FailureSeverity.WARNING,
        recoveryAction = RecoveryAction.CLEAR_INPUT
    )

    /** 备份 I/O 错误 */
    data class IoError(val reason: String) : BackupFailure(
        code = FailureCode.BACKUP_IO_ERROR,
        severity = FailureSeverity.ERROR,
        recoveryAction = RecoveryAction.RETRY
    )

    /** 导入冲突 — 目标库中存在同名条目 */
    data object ImportConflict : BackupFailure(
        code = FailureCode.BACKUP_IMPORT_CONFLICT,
        severity = FailureSeverity.WARNING,
        recoveryAction = RecoveryAction.NONE
    )
}
