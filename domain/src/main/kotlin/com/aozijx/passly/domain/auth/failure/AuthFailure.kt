package com.aozijx.passly.domain.auth.failure

import com.aozijx.passly.domain.failure.AppFailure
import com.aozijx.passly.domain.failure.FailureCode
import com.aozijx.passly.domain.failure.FailureOrigin
import com.aozijx.passly.domain.failure.FailureSeverity
import com.aozijx.passly.domain.failure.RecoveryAction
import java.util.UUID

/**
 * 认证领域失败类型。
 *
 * 不包含账号、密码、次数明细等敏感信息。
 */
sealed class AuthFailure(
    override val code: String,
    override val severity: FailureSeverity = FailureSeverity.INFO,
    override val recoveryAction: RecoveryAction = RecoveryAction.NONE,
    override val correlationId: String = UUID.randomUUID().toString()
) : AppFailure {
    override val origin: FailureOrigin get() = FailureOrigin.DOMAIN

    /** 凭据错误 — 密码、PIN、生物识别不匹配 */
    data object CredentialIncorrect : AuthFailure(
        code = FailureCode.AUTH_CREDENTIAL_INCORRECT,
        severity = FailureSeverity.INFO,
        recoveryAction = RecoveryAction.CLEAR_INPUT
    )

    /** 会话过期 — 需要重新认证 */
    data object SessionExpired : AuthFailure(
        code = FailureCode.AUTH_SESSION_EXPIRED,
        severity = FailureSeverity.INFO,
        recoveryAction = RecoveryAction.REAUTHENTICATE
    )

    /** 生物识别不可用 — 硬件/系统级 */
    data class BiometricUnavailable(val reason: String) : AuthFailure(
        code = FailureCode.AUTH_BIOMETRIC_UNAVAILABLE,
        severity = FailureSeverity.WARNING,
        recoveryAction = RecoveryAction.OPEN_SETTINGS
    )

    /** 锁定 — 多次尝试失败 */
    data object LockedOut : AuthFailure(
        code = FailureCode.AUTH_LOCKED_OUT,
        severity = FailureSeverity.WARNING,
        recoveryAction = RecoveryAction.REAUTHENTICATE
    )

    /** 恢复码无效 */
    data object RecoveryCodeInvalid : AuthFailure(
        code = FailureCode.AUTH_RECOVERY_INVALID,
        severity = FailureSeverity.INFO,
        recoveryAction = RecoveryAction.CLEAR_INPUT
    )

    /** 主密码强度不足 */
    data object MasterPasswordWeak : AuthFailure(
        code = FailureCode.AUTH_MASTER_PASSWORD_WEAK,
        severity = FailureSeverity.INFO,
        recoveryAction = RecoveryAction.CLEAR_INPUT
    )
}
