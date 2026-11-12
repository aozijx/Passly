package com.aozijx.passly.security.authentication.method.credential

import androidx.biometric.BiometricPrompt
import com.aozijx.passly.domain.access.model.AuthenticationKdfParameters
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationMethods
import com.aozijx.passly.domain.access.model.AuthenticationPendingMaintenance
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.AuthenticationSnapshot
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.model.BiometricBinding
import com.aozijx.passly.domain.access.model.BiometricBootstrapState
import com.aozijx.passly.domain.access.model.BiometricRotationJournal
import com.aozijx.passly.domain.access.model.BiometricRotationPhase
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.KeyEnvelope
import com.aozijx.passly.domain.access.model.SessionLockRequest
import com.aozijx.passly.domain.access.port.AuthenticationKdfParameterSource
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.port.AuthenticationRuntimeMaintenance
import com.aozijx.passly.domain.access.port.AuthenticationRuntimeMaintenanceResult
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import com.aozijx.passly.security.authentication.flow.AuthenticationAvailabilityResolverPort
import com.aozijx.passly.security.authentication.host.AuthHostSnapshot
import com.aozijx.passly.security.authentication.host.AuthUiHost
import com.aozijx.passly.security.authentication.host.AuthenticationHostRegistry
import com.aozijx.passly.security.authentication.host.BiometricHostResult
import com.aozijx.passly.security.authentication.host.BiometricPromptSpec
import com.aozijx.passly.security.authentication.host.SecretHostResult
import com.aozijx.passly.security.authentication.method.biometric.BiometricCryptoGateway
import com.aozijx.passly.security.authentication.method.biometric.BiometricCryptoPreparation
import com.aozijx.passly.security.authentication.method.biometric.RotateBiometricKeyUseCase
import com.aozijx.passly.security.dek.DekManager
import com.aozijx.passly.security.dek.DekUnlockResult
import com.aozijx.passly.security.dek.FieldKeyManager
import com.aozijx.passly.security.dek.InMemoryDekBootstrapStore
import com.aozijx.passly.testing.testLogger
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DefaultAuthenticationMethodProvisionerTest {
    @Test
    fun `rotate uses real host lease and reports both deferred maintenance obligations`() {
        runBlocking {
            val store = Store()
            val dekManager = DekManager(store, FieldKeyManager(), testLogger())
            check(
                dekManager.setDek(
                    EnvelopeType.APP_PASSWORD,
                    ByteArray(32) { it.toByte() },
                ) == DekUnlockResult.Success,
            )
            val maintenance =
                AuthenticationRuntimeMaintenance {
                    if (store.maintenanceCalls++ == 0) {
                        AuthenticationRuntimeMaintenanceResult.COMPLETE
                    } else {
                        AuthenticationRuntimeMaintenanceResult.RETRY_REQUIRED
                    }
                }
            val authenticationManager = FakeAuthenticationManager()
            val hostRegistry = AuthenticationHostRegistry()
            val host = FakeHost()
            val token = hostRegistry.register(host)
            val provisioner =
                DefaultAuthenticationMethodProvisioner(
                    kdfRunner = NoopKdfRunner(),
                    kdfConfiguration = FakeKdfConfiguration(),
                    dekManager = dekManager,
                    session = FakeProvisioningSession(),
                    authenticationManager = authenticationManager,
                    vaultBootstrapStore = store,
                    hostRegistry = hostRegistry,
                    rotateBiometricKey =
                        RotateBiometricKeyUseCase(
                            vaultBootstrapStore = store,
                            cryptoFactory = FakeCryptoGateway(),
                            dekManager = dekManager,
                            runtimeMaintenance = maintenance,
                        ),
                    runtimeMaintenance = maintenance,
                    availabilityResolver = FakeAvailabilityResolver(),
                )

            try {
                val result = provisioner.rotateBiometricPolicy(invalidateOnEnrollment = true)

                assertEquals(
                    AuthenticationResult.Success(
                        method = AuthenticationMethod.BIOMETRIC,
                        pendingMaintenance =
                            setOf(
                                AuthenticationPendingMaintenance.BIOMETRIC_ROTATION_CLEANUP,
                                AuthenticationPendingMaintenance.AUTHENTICATION_AVAILABILITY_REFRESH,
                            ),
                    ),
                    result,
                )
                assertEquals(1, store.commitCount)
                assertEquals(2, store.maintenanceCalls)
                assertEquals(1, authenticationManager.refreshCalls.get())
                assertEquals(BiometricRotationPhase.COMMITTED, store.state.rotation?.phase)
                assertEquals(store.state.rotation?.candidateAlias, store.state.binding?.activeAlias)
                assertEquals(setOf("old"), store.state.cleanupAliases)

                val releasedLease = withTimeoutOrNull(100) { hostRegistry.awaitLease(timeoutMs = 50) }
                assertNotNull("Provisioner must release the host lease", releasedLease)
                releasedLease?.release()
            } finally {
                hostRegistry.unregister(token)
            }
        }
    }

    private class FakeCryptoGateway : BiometricCryptoGateway {
        override fun baseAlias() = "test.biometric"

        override fun createDecrypt(alias: String, iv: ByteArray): BiometricCryptoPreparation =
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

        override fun deleteAlias(alias: String) = false
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

    private class FakeAuthenticationManager : AuthenticationManager {
        override val state: StateFlow<AuthenticationState> =
            MutableStateFlow(AuthenticationState.Authenticated(authenticatedAtMs = 1L))
        override val methods: StateFlow<AuthenticationMethods> =
            MutableStateFlow(AuthenticationMethods(setOf(AuthenticationMethod.BIOMETRIC)))
        val refreshCalls = AtomicInteger()

        override suspend fun authenticate(
            request: AuthenticationRequest,
            input: com.aozijx.passly.domain.access.model.AuthInput,
        ): AuthenticationResult = error("unused")

        override suspend fun lock(request: SessionLockRequest) = error("unused")

        override suspend fun refreshAvailability() {
            refreshCalls.incrementAndGet()
            error("availability refresh intentionally deferred")
        }

        override fun snapshot() = AuthenticationSnapshot(state.value, methods.value)
    }

    private class FakeProvisioningSession : AuthenticationProvisioningSession {
        override fun isRecoveryMode() = false

        override suspend fun lock(request: SessionLockRequest) = error("unused")

        override suspend fun markAuthenticated(purpose: AuthenticationPurpose) = true
    }

    private class FakeKdfConfiguration : AuthenticationKdfParameterSource {
        override suspend fun selected() =
            AuthenticationKdfParameters(
                version = 0x13,
                memoryKiB = 65_536,
                iterations = 3,
                parallelism = 4,
                keyLengthBits = 256,
            )
    }

    private class NoopKdfRunner : KdfRunner {
        override suspend fun <T> execute(secret: SecretChars, operation: KdfOperation<T>): T = operation.run(CharArray(0))

        override fun close() = Unit
    }

    private class FakeAvailabilityResolver : AuthenticationAvailabilityResolverPort {
        override suspend fun resolve() = AuthenticationMethods(setOf(AuthenticationMethod.BIOMETRIC))

        override suspend fun hasAlternativePrimaryFactor(excluding: EnvelopeType) = true
    }

    private class Store : VaultBootstrapStore by InMemoryDekBootstrapStore() {
        var state = BiometricBootstrapState(BiometricBinding("old", true), null, emptySet())
        var commitCount = 0
        var maintenanceCalls = 0

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
