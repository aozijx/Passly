package com.aozijx.passly.domain.access.model

import com.github.f4b6a3.uuid.UuidCreator

enum class AuthenticationMethod {
    BIOMETRIC,
    APP_PASSWORD,
    RECOVERY_CODE,
}

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
    RECOVER_AUTH_METHODS,
    CHANGE_BIOMETRIC_POLICY,
    EXPORT_DIAGNOSTICS,
    RECOVER_DATABASE,
    CLEAR_DATABASE,
}

@JvmInline
value class AuthenticationRequestId(val value: String) {
    init {
        require(value.isNotBlank()) { "Authentication request ID cannot be blank" }
    }

    companion object {
        fun new(): AuthenticationRequestId =
            AuthenticationRequestId(UuidCreator.getTimeOrderedEpoch().toString())
    }
}

data class AuthenticationRequest(
    val purpose: AuthenticationPurpose,
    val allowedMethods: Set<AuthenticationMethod> = AuthenticationMethod.entries.toSet(),
    val id: AuthenticationRequestId = AuthenticationRequestId.new(),
) {
    init {
        require(allowedMethods.isNotEmpty()) { "At least one authentication method is required" }
    }
}

sealed interface AuthenticationState {
    data object Locked : AuthenticationState

    data class Authenticating(
        val requestId: AuthenticationRequestId,
        val method: AuthenticationMethod,
    ) : AuthenticationState

    data class Authenticated(val authenticatedAtMs: Long) : AuthenticationState

    /** Database access is restricted to rebuilding primary authentication methods. */
    data class RecoveryMode(val authenticatedAtMs: Long) : AuthenticationState

    data class Locking(val reason: LockReason) : AuthenticationState
}

/** A set-based availability model avoids one Boolean property per authentication method. */
data class AuthenticationMethods(
    val available: Set<AuthenticationMethod> = emptySet(),
) {
    operator fun contains(method: AuthenticationMethod): Boolean = method in available

    fun restrictTo(methods: Set<AuthenticationMethod>): AuthenticationMethods =
        AuthenticationMethods(available intersect methods)
}

sealed interface AuthenticationResult {
    data class Success(
        val method: AuthenticationMethod,
        val reusedSession: Boolean = false,
    ) : AuthenticationResult

    data class Cancelled(val reason: CancellationReason) : AuthenticationResult
    data class Failure(val failure: AuthenticationFailure) : AuthenticationResult
}

enum class CancellationReason { USER, CALLER, SESSION_LOCKED }

enum class AuthenticationFailureCode {
    BUSY,
    METHOD_UNAVAILABLE,
    KEY_MISSING,
    KEY_INVALIDATED,
    CREDENTIAL_INCORRECT,
    PASSWORD_POLICY_VIOLATION,
    ENVELOPE_CORRUPTED,
    LAST_METHOD_REQUIRED,
    RATE_LIMITED,
    SESSION_MODE_RESTRICTED,
    SESSION_TRANSITION_FAILED,
}

data class AuthenticationFailure(
    val code: AuthenticationFailureCode,
    val requestId: AuthenticationRequestId? = null,
    val method: AuthenticationMethod? = null,
    val attempts: AttemptStatus = AttemptStatus(),
    val retryAfterMs: Long? = null,
) {
    init {
        require(retryAfterMs == null || retryAfterMs >= 0L) { "Retry delay cannot be negative" }
    }
}

data class AttemptStatus(
    val used: Int = 0,
    val limit: Int? = null,
) {
    init {
        require(used >= 0) { "Used attempts cannot be negative" }
        require(limit == null || limit > 0) { "Attempt limit must be positive" }
        require(limit == null || used <= limit) { "Used attempts cannot exceed the limit" }
    }

    val remaining: Int? get() = limit?.minus(used)
}

enum class LockReason {
    USER,
    IDLE_TIMEOUT,
    AUTOFILL_REQUEST_FINISHED,
    BACKGROUND,
    RECOVERY_EXIT,
    INTEGRITY_FAILURE,
    APP_EXIT,
}

data class AuthenticationSnapshot(
    val state: AuthenticationState,
    val methods: AuthenticationMethods,
)
