package com.aozijx.passly.security.authentication

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Commits the one-time recovery-code consumption or closes the staged session.
 *
 * The caller must invoke this after the recovery DEK has been accepted but before publishing
 * either recovery-mode or database-recovery success.
 */
internal suspend fun consumeRecoveryCodeOrRollback(
    consume: suspend () -> Unit,
    rollback: suspend () -> Unit
): Boolean = try {
    consume()
    true
} catch (cancelled: CancellationException) {
    withContext(NonCancellable) { rollback() }
    throw cancelled
} catch (_: Throwable) {
    withContext(NonCancellable) { rollback() }
    false
}

/**
 * A rebuilt primary password ends a recovery session before refreshing derived availability.
 */
internal suspend fun finishRecoveryPasswordProvisioning(
    seal: suspend () -> Unit,
    refreshAvailability: suspend () -> Unit
) {
    withContext(NonCancellable) { seal() }
    try {
        refreshAvailability()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Throwable) {
        // The envelope is authoritative. The next authentication request refreshes availability.
    }
}
