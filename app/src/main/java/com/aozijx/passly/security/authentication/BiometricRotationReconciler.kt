package com.aozijx.passly.security.authentication

import com.aozijx.passly.security.envelope.BiometricRotationPhase
import com.aozijx.passly.security.envelope.BootstrapStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricRotationReconciler @Inject constructor(
    private val bootstrapStore: BootstrapStore,
    private val cryptoFactory: BiometricCryptoFactory
) {
    suspend fun reconcile() {
        val state = bootstrapStore.loadBiometricState()
        val rotation = state.rotation
        if (rotation?.phase == BiometricRotationPhase.PREPARED &&
            state.binding?.activeAlias != rotation.candidateAlias
        ) {
            if (cryptoFactory.deleteAlias(rotation.candidateAlias)) {
                bootstrapStore.clearBiometricRotationJournal()
            }
        }
        state.cleanupAliases.forEach { alias ->
            if (alias != state.binding?.activeAlias && cryptoFactory.deleteAlias(alias)) {
                bootstrapStore.clearBiometricCleanupAlias(alias)
            }
        }
        if (rotation?.phase == BiometricRotationPhase.COMMITTED) {
            bootstrapStore.clearBiometricRotationJournal()
        }
    }
}
