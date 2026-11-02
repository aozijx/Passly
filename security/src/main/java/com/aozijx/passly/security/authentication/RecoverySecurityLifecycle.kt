package com.aozijx.passly.security.authentication

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
) {
    withContext(NonCancellable) { seal() }
    try {
        refreshAvailability()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        // The envelope is authoritative. The next authentication request refreshes availability.
    }
}
