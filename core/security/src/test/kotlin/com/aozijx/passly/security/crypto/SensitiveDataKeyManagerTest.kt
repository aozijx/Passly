package com.aozijx.passly.security.crypto

import com.aozijx.passly.core.telemetry.TelemetryReporter
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.KeyEnvelope
import com.aozijx.passly.security.envelope.BiometricBootstrapState
import com.aozijx.passly.security.envelope.BiometricBinding
import com.aozijx.passly.security.envelope.BiometricRotationJournal
import com.aozijx.passly.security.envelope.BootstrapStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SensitiveDataKeyManagerTest {
    private lateinit var store: InMemoryBootstrapStore
    private lateinit var scope: CoroutineScope
    private lateinit var manager: SensitiveDataKeyManager

    @Before
    fun setUp() = runBlocking {
        store = InMemoryBootstrapStore()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val dekManager = DekManager(store, SessionKeyManager(), TelemetryReporter { })
        val result = dekManager.setDek(EnvelopeType.APP_PASSWORD, ByteArray(32) { it.toByte() })
        check(result == UnlockResult.Success)
        manager = SensitiveDataKeyManager(store, dekManager, scope)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `provisioning persists only a wrapped stable key`() = runBlocking {
        val first = manager.withProvisionedKey(ByteArray::clone)
        val envelope = store.sensitiveKeyEnvelope

        assertNotNull(envelope)
        assertFalse(envelope!!.containsSubsequence(first))

        val second = manager.withProvisionedKey(ByteArray::clone)
        assertArrayEquals(first, second)
        first.fill(0)
        second.fill(0)
    }

    @Test
    fun `read access requires fresh authentication and clear revokes it`() = runBlocking {
        assertFreshAuthenticationRequired()

        manager.unlockAfterFreshAuthentication()
        val unlocked = manager.withUnlockedKey(ByteArray::clone)
        assertTrue(unlocked.any { it != 0.toByte() })
        unlocked.fill(0)

        manager.clear()
        assertFreshAuthenticationRequired()
    }

    @Test
    fun `fresh authentication expires after ttl`() = runBlocking {
        manager.unlockAfterFreshAuthentication(ttlMs = 20L)
        manager.withUnlockedKey { assertTrue(it.isNotEmpty()) }

        delay(100L)

        assertFreshAuthenticationRequired()
    }

    private suspend fun assertFreshAuthenticationRequired() {
        val failure = runCatching { manager.withUnlockedKey { Unit } }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
    }

    private fun ByteArray.containsSubsequence(candidate: ByteArray): Boolean {
        if (candidate.isEmpty() || candidate.size > size) return false
        return indices
            .take(size - candidate.size + 1)
            .any { offset -> candidate.indices.all { this[offset + it] == candidate[it] } }
    }

    private class InMemoryBootstrapStore : BootstrapStore {
        private val envelopes = linkedMapOf<EnvelopeType, KeyEnvelope>()
        private var verificationTag: ByteArray? = null
        var sensitiveKeyEnvelope: ByteArray? = null
            private set

        override suspend fun save(envelope: KeyEnvelope) {
            envelopes[envelope.type] = envelope
        }

        override suspend fun load(type: EnvelopeType): KeyEnvelope? = envelopes[type]

        override suspend fun loadAll(): List<KeyEnvelope> = envelopes.values.toList()

        override suspend fun delete(type: EnvelopeType) {
            envelopes.remove(type)
        }

        override suspend fun saveVerificationTag(tag: ByteArray) {
            verificationTag = tag.clone()
        }

        override suspend fun loadVerificationTag(): ByteArray? = verificationTag?.clone()

        override suspend fun saveSensitiveKeyEnvelope(envelope: ByteArray) {
            sensitiveKeyEnvelope = envelope.clone()
        }

        override suspend fun loadSensitiveKeyEnvelope(): ByteArray? = sensitiveKeyEnvelope?.clone()

        override suspend fun loadBiometricState() = BiometricBootstrapState(
            binding = null,
            rotation = null,
            cleanupAliases = emptySet()
        )

        override suspend fun prepareBiometricRotation(journal: BiometricRotationJournal) = Unit

        override suspend fun commitBiometricRotation(
            envelope: KeyEnvelope,
            binding: BiometricBinding,
            obsoleteAlias: String?
        ) = Unit

        override suspend fun clearBiometricCleanupAlias(alias: String) = Unit

        override suspend fun disableBiometric(activeAlias: String) = Unit

        override suspend fun clearBiometricRotationJournal() = Unit

        override suspend fun clear() {
            envelopes.clear()
            verificationTag?.fill(0)
            verificationTag = null
            sensitiveKeyEnvelope?.fill(0)
            sensitiveKeyEnvelope = null
        }
    }
}
