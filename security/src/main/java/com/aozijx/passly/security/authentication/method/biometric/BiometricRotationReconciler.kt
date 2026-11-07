package com.aozijx.passly.security.authentication.method.biometric
import com.aozijx.passly.domain.access.model.BiometricRotationPhase
import com.aozijx.passly.domain.access.port.AuthenticationRuntimeMaintenance
import com.aozijx.passly.domain.access.port.AuthenticationRuntimeMaintenanceResult
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import com.aozijx.passly.security.session.SecuritySessionEventLogger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal suspend fun reconcileBiometricRotationState(
    store: VaultBootstrapStore,
    deleteAlias: (String) -> Boolean,
): AuthenticationRuntimeMaintenanceResult {
    val state = store.loadBiometricState()
    val rotation = state.rotation
    var retryRequired = false
    if (rotation?.phase == BiometricRotationPhase.PREPARED) {
        if (state.binding?.activeAlias == rotation.candidateAlias || deleteAlias(rotation.candidateAlias)) {
            store.clearBiometricRotationJournal()
        } else {
            retryRequired = true
        }
    }
    state.cleanupAliases.forEach { alias ->
        when {
            alias == state.binding?.activeAlias -> retryRequired = true
            deleteAlias(alias) -> store.clearBiometricCleanupAlias(alias)
            else -> retryRequired = true
        }
    }
    if (rotation?.phase == BiometricRotationPhase.COMMITTED && !retryRequired) {
        store.clearBiometricRotationJournal()
    }
    return if (retryRequired) {
        AuthenticationRuntimeMaintenanceResult.RETRY_REQUIRED
    } else {
        AuthenticationRuntimeMaintenanceResult.COMPLETE
    }
}

@Singleton
internal class BiometricRotationReconciler
    @Inject
    constructor(
        private val vaultBootstrapStore: VaultBootstrapStore,
        private val cryptoFactory: BiometricCryptoFactory,
        private val securityEvents: SecuritySessionEventLogger,
    ) : AuthenticationRuntimeMaintenance {
        private val reconciliationMutex = Mutex()

        override suspend fun reconcileStartupState(): AuthenticationRuntimeMaintenanceResult =
            reconciliationMutex.withLock {
                try {
                    reconcileBiometricRotationState(vaultBootstrapStore, cryptoFactory::deleteAlias).also { result ->
                        if (result == AuthenticationRuntimeMaintenanceResult.RETRY_REQUIRED) {
                            recordDeferredCleanup()
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    recordDeferredCleanup(failure)
                    AuthenticationRuntimeMaintenanceResult.RETRY_REQUIRED
                }
            }

        private suspend fun recordDeferredCleanup(failure: Exception? = null) {
            try {
                securityEvents.recordBiometricRotationCleanupDeferred(failure)
            } catch (cancelled: CancellationException) {
                failure?.let(cancelled::addSuppressed)
                throw cancelled
            } catch (fatal: Error) {
                failure?.let(fatal::addSuppressed)
                throw fatal
            } catch (loggingFailure: Exception) {
                failure?.let { if (it !== loggingFailure) it.addSuppressed(loggingFailure) }
            }
        }
    }
