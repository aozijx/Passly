package com.aozijx.passly.domain.authentication

import com.aozijx.passly.core.diagnostics.AppFailure
import com.aozijx.passly.core.diagnostics.FailureOrigin
import com.aozijx.passly.core.diagnostics.FailureSeverity
import com.aozijx.passly.core.diagnostics.RecoveryAction
import java.util.UUID

enum class AuthenticationMethod { BIOMETRIC, APP_PASSWORD, RECOVERY_CODE }

enum class AuthenticationPurpose {
    UNLOCK_VAULT,
    REAUTHENTICATE,
    AUTOFILL,
    REVEAL_SECRET,
    DELETE_ENTRY,
    BACKUP_EXPORT,
    BACKUP_IMPORT,
    MANAGE_APP_PASSWORD,
    MANAGE_RECOVERY_CODE,
    CHANGE_BIOMETRIC_POLICY,
    EXPORT_DIAGNOSTICS
}

data class AuthenticationRequest(
    val purpose: AuthenticationPurpose,
    val allowedMethods: Set<AuthenticationMethod> = if (
        purpose == AuthenticationPurpose.UNLOCK_VAULT
    ) {
        AuthenticationMethod.entries.toSet()
    } else {
        setOf(AuthenticationMethod.BIOMETRIC, AuthenticationMethod.APP_PASSWORD)
    },
    val requireFreshAuthentication: Boolean = purpose != AuthenticationPurpose.UNLOCK_VAULT,
    val correlationId: String = UUID.randomUUID().toString()
) {
    init {
        require(allowedMethods.isNotEmpty()) { "At least one authentication method is required" }
        if (AuthenticationMethod.RECOVERY_CODE in allowedMethods) {
            require(purpose == AuthenticationPurpose.UNLOCK_VAULT) {
                "Recovery code may only unlock the vault"
            }
        }
    }
}

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
    override val recoveryAction: RecoveryAction = RecoveryAction.RETRY,
    override val safeFields: Map<String, String> = emptyMap()
) : AppFailure {
    override val code: String = "AUTH_${authCode.name}"
}

enum class LockReason { USER, IDLE_TIMEOUT, BACKGROUND, INTEGRITY_FAILURE, APP_EXIT }

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
