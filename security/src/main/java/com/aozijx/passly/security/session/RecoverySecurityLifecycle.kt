package com.aozijx.passly.security.session

import com.aozijx.passly.domain.access.model.AuthenticationPendingMaintenance
import com.aozijx.passly.security.authentication.flow.addSuppressedIfDistinct
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Commits the one-time recovery-code consumption or closes the staged session.
 *
 * The caller must invoke this after the recovery DEK has been accepted but before publishing
 * recovery-mode success.
 */
internal suspend fun consumeRecoveryCodeOrRollback(
    consume: suspend () -> Unit,
    rollback: suspend () -> Unit,
): Boolean =
    try {
        consume()
        true
    } catch (failure: Throwable) {
        val rollbackFailure =
            try {
                withContext(NonCancellable) { rollback() }
                null
            } catch (secondary: Throwable) {
                secondary
            }
        if (failure is CancellationException || failure is Error) {
            rollbackFailure?.let(failure::addSuppressedIfDistinct)
            throw failure
        }
        if (rollbackFailure != null) {
            rollbackFailure.addSuppressedIfDistinct(failure)
            throw rollbackFailure
        }
        false
    }

/**
 * A rebuilt primary password ends a recovery session before refreshing derived availability.
 */
internal suspend fun finishRecoveryPasswordProvisioning(
    seal: suspend () -> Unit,
    refreshAvailability: suspend () -> Unit,
): Set<AuthenticationPendingMaintenance> {
    withContext(NonCancellable) { seal() }
    try {
        refreshAvailability()
        return emptySet()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        return setOf(AuthenticationPendingMaintenance.AUTHENTICATION_AVAILABILITY_REFRESH)
    }
}
