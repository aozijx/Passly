package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

internal suspend fun withBiometricRotationCandidate(
    store: VaultBootstrapStore,
    candidateAlias: String,
    deleteAlias: (String) -> Boolean,
    operation: suspend () -> AuthenticationResult,
): AuthenticationResult {
    val result = try {
        operation()
    } catch (error: Throwable) {
        try {
            discardUncommittedCandidate(store, candidateAlias, deleteAlias)
        } catch (cleanup: Throwable) {
            if (cleanup !== error) error.addSuppressed(cleanup)
        }
        throw error
    }
    discardUncommittedCandidate(store, candidateAlias, deleteAlias)
    return result
}

private suspend fun discardUncommittedCandidate(
    store: VaultBootstrapStore,
    candidateAlias: String,
    deleteAlias: (String) -> Boolean,
) = withContext(NonCancellable) {
    val current = store.loadBiometricState()
    // An atomic commit may have completed even when its caller received cancellation.
    if (current.binding?.activeAlias == candidateAlias) return@withContext
    if (!deleteAlias(candidateAlias)) return@withContext
    if (current.rotation?.candidateAlias == candidateAlias) store.clearBiometricRotationJournal()
    if (candidateAlias in current.cleanupAliases) store.clearBiometricCleanupAlias(candidateAlias)
}
