package com.aozijx.passly.domain.authentication

/**
 * Central, non-overridable authentication-method policy for every purpose.
 *
 * This policy is intentionally not configurable and cannot be circumvented by
 * callers.  The [AuthenticationRequest.allowedMethods] can only narrow the
 * available set, never broaden it.
 */
object AuthenticationMethodPolicy {

    /** Primary authentication factors that represent everyday unlock methods. */
    val PRIMARY_METHODS: Set<AuthenticationMethod> = setOf(
        AuthenticationMethod.BIOMETRIC,
        AuthenticationMethod.APP_PASSWORD
    )

    /**
     * Purposes that are allowed during a recovery-mode session.
     *
     * A recovery-mode session is a restricted session entered by verifying a
     * recovery code.  Only these purposes are permitted while in recovery mode;
     * any other purpose will fail with [AuthenticationFailureCode.SESSION_MODE_RESTRICTED].
     */
    val RECOVERY_MODE_PURPOSES: Set<AuthenticationPurpose> = setOf(
        AuthenticationPurpose.RECOVERY_EXPORT,
        AuthenticationPurpose.RECOVER_AUTH_METHODS
    )

    /**
     * Purposes that can reuse an existing recovery-mode session without
     * re-authentication.
     */
    val RECOVERY_MODE_REUSABLE_PURPOSES: Set<AuthenticationPurpose> = setOf(
        AuthenticationPurpose.RECOVERY_EXPORT,
        AuthenticationPurpose.RECOVER_AUTH_METHODS
    )

    /**
     * Returns the set of [AuthenticationMethod]s authorized for the given [purpose].
     *
     * The caller's [AuthenticationRequest.allowedMethods] can only narrow this
     * set; it cannot allow methods that the policy forbids.
     */
    fun allowedAuthenticationMethods(purpose: AuthenticationPurpose): Set<AuthenticationMethod> =
        when (purpose) {
            AuthenticationPurpose.UNLOCK_VAULT,
            AuthenticationPurpose.BACKUP_EXPORT,
            AuthenticationPurpose.CLEAR_DATABASE -> PRIMARY_METHODS

            AuthenticationPurpose.RECOVERY_EXPORT,
            AuthenticationPurpose.RECOVER_AUTH_METHODS -> setOf(AuthenticationMethod.RECOVERY_CODE)

            AuthenticationPurpose.RECOVER_DATABASE -> AuthenticationMethod.entries.toSet()
            else -> PRIMARY_METHODS
        }

    /**
     * Returns whether the given [purpose] requires a fresh authentication
     * (i.e., cannot reuse an existing session).
     */
    fun requiresFreshAuthentication(
        purpose: AuthenticationPurpose,
        reauthenticateSensitiveCopies: Boolean
    ): Boolean = when (purpose) {
        AuthenticationPurpose.UNLOCK_VAULT,
        AuthenticationPurpose.REVEAL_SECRET -> false

        AuthenticationPurpose.COPY_SECRET -> reauthenticateSensitiveCopies
        AuthenticationPurpose.REVEAL_HIGH_SENSITIVITY_SECRET -> true
        else -> true
    }
}