package com.aozijx.passly.domain.authentication

import com.aozijx.passly.domain.failure.AppFailure
import com.aozijx.passly.domain.failure.FailureOrigin
import com.aozijx.passly.domain.failure.FailureSeverity
import com.aozijx.passly.domain.failure.RecoveryAction
import com.github.f4b6a3.uuid.UuidCreator

enum class AuthenticationMethod { BIOMETRIC, APP_PASSWORD, RECOVERY_CODE }

enum class SensitiveAccessLevel { STANDARD, HIGH }
enum class SensitiveAccessAction { REVEAL, COPY }

enum class AuthenticationPurpose {
    UNLOCK_VAULT,
    REAUTHENTICATE,
    AUTOFILL,
    REVEAL_SECRET,
    REVEAL_HIGH_SENSITIVITY_SECRET,
    COPY_SECRET,
    DELETE_ENTRY,
    BACKUP_EXPORT,
    BACKUP_IMPORT,
    MANAGE_APP_PASSWORD,
    MANAGE_RECOVERY_CODE,
    CHANGE_BIOMETRIC_POLICY,
    EXPORT_DIAGNOSTICS,
    RECOVER_DATABASE,
    CLEAR_DATABASE
}

/**
 * 认证请求。
 *
 * 调用者指定 [purpose] 和可选的 [correlationId]。
 * [allowedMethods] 是调用者的偏好约束（缩小可选范围），
 * 最终可用的认证方式由策略引擎与调用者约束的交集决定。
 *
 * 安全不变量：调用者无法覆盖。
 * - [requireFreshAuthentication] 由 [purpose] 决定，调用者不可控
 * - [allowedMethods] 最终受策略约束，调用者不能扩权
 */
data class AuthenticationRequest(
    val purpose: AuthenticationPurpose,
    val allowedMethods: Set<AuthenticationMethod> = AuthenticationMethod.entries.toSet(),
    val correlationId: String = UuidCreator.getTimeOrderedEpoch().toString()
)

sealed interface AuthenticationState {
    data object Locked : AuthenticationState
    data class AwaitingHost(val correlationId: String) : AuthenticationState
    data class Authenticating(
        val correlationId: String,
        val method: AuthenticationMethod
    ) : AuthenticationState
    data class Unlocking(val correlationId: String) : AuthenticationState
    data class Authenticated(val authenticatedAtMs: Long) : AuthenticationState
    data class Locking(val reason: LockReason) : AuthenticationState
}

data class AuthMethodAvailability(
    val biometric: Boolean = false,
    val appPassword: Boolean = false,
    val recoveryCode: Boolean = false
) {
    fun available(method: AuthenticationMethod): Boolean = when (method) {
        AuthenticationMethod.BIOMETRIC -> biometric
        AuthenticationMethod.APP_PASSWORD -> appPassword
        AuthenticationMethod.RECOVERY_CODE -> recoveryCode
    }
}

sealed interface AuthenticationResult {
    data class Success(
        val method: AuthenticationMethod,
        val reusedSession: Boolean = false
    ) : AuthenticationResult

    data class Cancelled(val byUser: Boolean) : AuthenticationResult
    data class Failure(val failure: AuthenticationFailure) : AuthenticationResult
}

enum class AuthenticationFailureCode {
    BUSY,
    HOST_UNAVAILABLE,
    METHOD_UNAVAILABLE,
    KEY_MISSING,
    KEY_INVALIDATED,
    CRYPTO_OBJECT_INVALID,
    CREDENTIAL_INCORRECT,
    PASSWORD_POLICY_VIOLATION,
    ENVELOPE_CORRUPTED,
    LAST_METHOD_REQUIRED,
    RATE_LIMITED,
    SESSION_TRANSITION_FAILED
}

data class AuthenticationFailure(
    val authCode: AuthenticationFailureCode,
    override val correlationId: String,
    override val origin: FailureOrigin = FailureOrigin.SECURITY,
    override val severity: FailureSeverity = FailureSeverity.ERROR,
    override val recoveryAction: RecoveryAction = RecoveryAction.RETRY
) : AppFailure {
    override val code: String = "AUTH_${authCode.name}"
}

enum class LockReason {
    USER,
    IDLE_TIMEOUT,
    AUTOFILL_REQUEST_FINISHED,
    BACKGROUND,
    INTEGRITY_FAILURE,
    APP_EXIT,
}

data class AuthenticationSnapshot(
    val state: AuthenticationState,
    val activeCorrelationId: String?,
    val authenticatedAtMs: Long?
)

fun interface AuthenticationCallback {
    fun onResult(result: AuthenticationResult)
}

interface AuthenticationRequestHandle {
    val correlationId: String
    fun cancel()
}
