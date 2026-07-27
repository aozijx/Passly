package com.aozijx.passly.domain.entry.failure

import com.aozijx.passly.domain.failure.AppFailure
import com.aozijx.passly.domain.failure.FailureCode
import com.aozijx.passly.domain.failure.FailureOrigin
import com.aozijx.passly.domain.failure.FailureSeverity
import com.aozijx.passly.domain.failure.RecoveryAction
import java.util.UUID

/**
 * 条目领域失败类型。
 */
sealed class EntryFailure(
    override val code: String,
    override val severity: FailureSeverity = FailureSeverity.ERROR,
    override val recoveryAction: RecoveryAction = RecoveryAction.NONE,
    override val correlationId: String = UUID.randomUUID().toString()
) : AppFailure {
    override val origin: FailureOrigin get() = FailureOrigin.DOMAIN

    /** 条目不存在 */
    data object NotFound : EntryFailure(
        code = FailureCode.ENTRY_NOT_FOUND,
        severity = FailureSeverity.WARNING,
        recoveryAction = RecoveryAction.NONE
    )

    /** 重复条目 */
    data object Duplicate : EntryFailure(
        code = FailureCode.ENTRY_DUPLICATE,
        severity = FailureSeverity.WARNING,
        recoveryAction = RecoveryAction.NONE
    )

    /** 校验失败 */
    data class ValidationFailed(val field: String) : EntryFailure(
        code = FailureCode.ENTRY_VALIDATION_FAILED,
        severity = FailureSeverity.INFO,
        recoveryAction = RecoveryAction.CLEAR_INPUT
    )

    /** 存储失败 */
    data class StorageFailed(val reason: String) : EntryFailure(
        code = FailureCode.ENTRY_STORAGE_FAILED,
        severity = FailureSeverity.ERROR,
        recoveryAction = RecoveryAction.RETRY
    )

    /** 版本冲突 */
    data object VersionConflict : EntryFailure(
        code = FailureCode.ENTRY_VERSION_CONFLICT,
        severity = FailureSeverity.WARNING,
        recoveryAction = RecoveryAction.NONE
    )
}
