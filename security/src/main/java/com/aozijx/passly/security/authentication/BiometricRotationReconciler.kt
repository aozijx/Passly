package com.aozijx.passly.security.authentication

import com.aozijx.passly.domain.access.model.BiometricRotationPhase
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricRotationReconciler @Inject constructor(
    private val vaultBootstrapStore: VaultBootstrapStore,
    private val cryptoFactory: BiometricCryptoFactory
) {
    suspend fun reconcile() {
        val state = vaultBootstrapStore.loadBiometricState()
        val rotation = state.rotation
        if (rotation?.phase == BiometricRotationPhase.PREPARED &&
            state.binding?.activeAlias != rotation.candidateAlias
        ) {
            if (cryptoFactory.deleteAlias(rotation.candidateAlias)) {
                vaultBootstrapStore.clearBiometricRotationJournal()
            }
        }
        state.cleanupAliases.forEach { alias ->
            if (alias != state.binding?.activeAlias && cryptoFactory.deleteAlias(alias)) {
                vaultBootstrapStore.clearBiometricCleanupAlias(alias)
            }
        }
        if (rotation?.phase == BiometricRotationPhase.COMMITTED) {
            vaultBootstrapStore.clearBiometricRotationJournal()
        }
    }
}
