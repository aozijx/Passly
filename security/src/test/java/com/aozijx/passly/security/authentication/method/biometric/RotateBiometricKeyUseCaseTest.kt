package com.aozijx.passly.security.authentication.method.biometric

import androidx.biometric.BiometricPrompt
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationPendingMaintenance
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.BiometricBinding
import com.aozijx.passly.domain.access.model.BiometricBootstrapState
import com.aozijx.passly.domain.access.model.BiometricRotationJournal
import com.aozijx.passly.domain.access.model.BiometricRotationPhase
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.KeyEnvelope
import com.aozijx.passly.domain.access.port.AuthenticationRuntimeMaintenance
import com.aozijx.passly.domain.access.port.AuthenticationRuntimeMaintenanceResult
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import com.aozijx.passly.security.authentication.host.AuthHostSnapshot
import com.aozijx.passly.security.authentication.host.AuthUiHost
import com.aozijx.passly.security.authentication.host.BiometricHostResult
import com.aozijx.passly.security.authentication.host.BiometricPromptSpec
import com.aozijx.passly.security.authentication.host.SecretHostResult
import com.aozijx.passly.security.dek.DekManager
import com.aozijx.passly.security.dek.DekUnlockResult
import com.aozijx.passly.security.dek.FieldKeyManager
import com.aozijx.passly.security.dek.InMemoryDekBootstrapStore
import com.aozijx.passly.testing.testLogger
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RotateBiometricKeyUseCaseTest {
    @Test
    fun `committed rotation succeeds while deferred cleanup is reported for retry`() =
        runBlocking {
            val store = Store()
            val dekManager = DekManager(store, FieldKeyManager(), testLogger())
            check(dekManager.setDek(EnvelopeType.APP_PASSWORD, ByteArray(32) { it.toByte() }) == DekUnlockResult.Success)
            var maintenanceCalls = 0
            val maintenance =
                AuthenticationRuntimeMaintenance {
                    maintenanceCalls += 1
                    if (maintenanceCalls == 1) {
                        AuthenticationRuntimeMaintenanceResult.COMPLETE
                    } else {
                        AuthenticationRuntimeMaintenanceResult.RETRY_REQUIRED
                    }
                }
            val useCase =
                RotateBiometricKeyUseCase(
                    vaultBootstrapStore = store,
                    cryptoFactory = FakeCryptoGateway(),
                    dekManager = dekManager,
                    runtimeMaintenance = maintenance,
                )

            val result = useCase.rotate(FakeHost(), invalidateOnEnrollment = true, correlationId = "request")

            assertEquals(
                AuthenticationResult.Success(
                    method = AuthenticationMethod.BIOMETRIC,
                    pendingMaintenance = setOf(AuthenticationPendingMaintenance.BIOMETRIC_ROTATION_CLEANUP),
                ),
                result,
            )
            assertEquals(2, maintenanceCalls)
            assertEquals(1, store.commitCount)
            assertEquals(BiometricRotationPhase.COMMITTED, store.state.rotation?.phase)
            assertEquals(store.state.rotation?.candidateAlias, store.state.binding?.activeAlias)
            assertEquals(setOf("old"), store.state.cleanupAliases)
            assertTrue(store.deletedAliases.isEmpty())

            val retryAttempt =
                useCase.rotate(
                    FakeHost(),
                    invalidateOnEnrollment = true,
                    correlationId = "retry",
                )

            assertTrue(retryAttempt is AuthenticationResult.Failure)
            assertEquals(1, store.commitCount)
            assertEquals(3, maintenanceCalls)
        }

    private class FakeCryptoGateway : BiometricCryptoGateway {
        override fun baseAlias() =
            "test.biometric"

        override fun createDecrypt(
            alias: String,
            iv: ByteArray,
        ): BiometricCryptoPreparation =
            BiometricCryptoPreparation.KeyMissing

        override fun createEncrypt(
            alias: String,
            invalidateOnEnrollment: Boolean,
        ): BiometricCryptoPreparation =
            BiometricCryptoPreparation.Ready(
                Cipher.getInstance("AES/GCM/NoPadding").apply {
                    init(Cipher.ENCRYPT_MODE, SecretKeySpec(ByteArray(16) { 7 }, "AES"))
                },
            )

        override fun deleteAlias(alias: String): Boolean = false
    }

    private class FakeHost : AuthUiHost {
        override val ownerId = "test-owner"

        override fun snapshot() = AuthHostSnapshot(resumed = true, finishing = false, destroyed = false)

        override suspend fun chooseMethod(
            purpose: AuthenticationPurpose,
            methods: List<AuthenticationMethod>,
        ): AuthenticationMethod? = error("unused")

        override suspend fun requestSecret(
            purpose: AuthenticationPurpose,
            method: AuthenticationMethod,
        ): SecretHostResult = error("unused")

        override suspend fun authenticateBiometric(
            spec: BiometricPromptSpec,
            cryptoObject: BiometricPrompt.CryptoObject?,
        ): BiometricHostResult =
            BiometricHostResult.Success(
                BiometricPrompt.AuthenticationResult::class.java
                    .getDeclaredConstructor(BiometricPrompt.CryptoObject::class.java, Int::class.javaPrimitiveType)
                    .apply { isAccessible = true }
                    .newInstance(cryptoObject, BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC),
            )
    }

    private class Store : VaultBootstrapStore by InMemoryDekBootstrapStore() {
        var state = BiometricBootstrapState(BiometricBinding("old", true), null, emptySet())
        var commitCount = 0
        val deletedAliases = mutableListOf<String>()

        override suspend fun loadBiometricState() = state

        override suspend fun prepareBiometricRotation(journal: BiometricRotationJournal) {
            state = state.copy(rotation = journal)
        }

        override suspend fun commitBiometricRotation(
            envelope: KeyEnvelope,
            binding: BiometricBinding,
            obsoleteAlias: String?,
        ) {
            commitCount += 1
            state =
                state.copy(
                    binding = binding,
                    rotation = requireNotNull(state.rotation).copy(phase = BiometricRotationPhase.COMMITTED),
                    cleanupAliases = obsoleteAlias?.let { state.cleanupAliases + it } ?: state.cleanupAliases,
                )
        }

        override suspend fun clearBiometricCleanupAlias(alias: String) {
            state = state.copy(cleanupAliases = state.cleanupAliases - alias)
        }

        override suspend fun clearBiometricRotationJournal() {
            state = state.copy(rotation = null)
        }
    }
}
