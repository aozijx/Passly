package com.aozijx.passly.security.dek

import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.FreshAuthenticationRequiredException
import com.aozijx.passly.testing.testLogger
import javax.crypto.AEADBadTagException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SensitiveDataKeyManagerTest {
    private lateinit var store: InMemoryDekBootstrapStore
    private lateinit var scope: CoroutineScope
    private lateinit var dekManager: DekManager
    private lateinit var manager: SensitiveDataKeyManager

    @Before
    fun setUp() =
        runBlocking {
            store = InMemoryDekBootstrapStore()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            dekManager = DekManager(store, FieldKeyManager(), testLogger())
            val result = dekManager.setDek(EnvelopeType.APP_PASSWORD, ByteArray(32) { it.toByte() })
            check(result == DekUnlockResult.Success)
            manager = SensitiveDataKeyManager(store, dekManager, scope, CapturingSecureRandom())
        }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `provisioning persists only a wrapped stable key`() =
        runBlocking {
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
    fun `sensitive field decryption requires fresh authentication`() =
        runBlocking {
            val encryptor = SecuritySensitiveFieldCryptor(manager)
            val aad = byteArrayOf(3, 1, 4)
            val ciphertext = encryptor.encrypt("secret", aad)

            val lockedFailure = runCatching { encryptor.decrypt(ciphertext, aad) }.exceptionOrNull()
            assertEquals(FreshAuthenticationRequiredException::class, lockedFailure!!::class)

            manager.unlockAfterFreshAuthentication()

            assertEquals("secret", encryptor.decrypt(ciphertext, aad))
            val wrongAadFailure = runCatching { encryptor.decrypt(ciphertext, byteArrayOf(2)) }.exceptionOrNull()
            assertEquals(AEADBadTagException::class, wrongAadFailure!!::class)
            manager.clear()
        }

    @Test
    fun `new key is wiped when nonce generation fails before handoff`() =
        runBlocking {
            val injectedFailure = IllegalStateException("nonce generation failed")
            val random = CapturingSecureRandom(failingCall = 2, failure = injectedFailure)
            val candidate = SensitiveDataKeyManager(store, dekManager, scope, random)

            val failure =
                withContext(NonCancellable) {
                    runCatching { candidate.withProvisionedKey { Unit } }.exceptionOrNull()
                }

            assertSame(injectedFailure, failure)
            assertEquals(2, random.buffers.size)
            random.buffers.forEach { assertArrayEquals(ByteArray(it.size), it) }
        }

    @Test
    fun `new key is wiped when random generation fails partway through`() =
        runBlocking {
            val injectedFailure = IllegalStateException("key generation failed")
            val random = CapturingSecureRandom(failingCall = 1, failure = injectedFailure)
            val candidate = SensitiveDataKeyManager(store, dekManager, scope, random)

            val failure =
                withContext(NonCancellable) {
                    runCatching { candidate.withProvisionedKey { Unit } }.exceptionOrNull()
                }

            assertSame(injectedFailure, failure)
            assertEquals(1, random.buffers.size)
            assertArrayEquals(ByteArray(random.buffers.first().size), random.buffers.first())
        }

    @Test
    fun `new key is wiped when envelope persistence fails before handoff`() =
        runBlocking {
            val random = CapturingSecureRandom()
            val candidate = SensitiveDataKeyManager(store, dekManager, scope, random)
            val injectedFailure = IllegalStateException("sensitive envelope save failed")
            store.sensitiveKeySaveFailure = injectedFailure

            val failure =
                withContext(NonCancellable) {
                    runCatching { candidate.withProvisionedKey { Unit } }.exceptionOrNull()
                }

            assertSame(injectedFailure, failure)
            assertEquals(2, random.buffers.size)
            random.buffers.forEach { assertArrayEquals(ByteArray(it.size), it) }
        }

    @Test
    fun `new key is wiped when envelope persistence is cancelled or fatal`() =
        runBlocking {
            listOf(CancellationException("save cancelled"), AssertionError("fatal save failure")).forEach { injectedFailure ->
                val random = CapturingSecureRandom()
                val candidate = SensitiveDataKeyManager(store, dekManager, scope, random)
                store.sensitiveKeySaveFailure = injectedFailure

                val observed =
                    withContext(NonCancellable) {
                        runCatching { candidate.withProvisionedKey { Unit } }.exceptionOrNull()
                    }

                assertSame(injectedFailure, observed)
                assertEquals(2, random.buffers.size)
                random.buffers.forEach { assertArrayEquals(ByteArray(it.size), it) }
            }
            store.sensitiveKeySaveFailure = null
        }

    @Test
    fun `new key is wiped after successful caller handoff`() =
        runBlocking {
            val random = CapturingSecureRandom()
            val candidate = SensitiveDataKeyManager(store, dekManager, scope, random)

            candidate.withProvisionedKey { key ->
                assertTrue(key.any { it != 0.toByte() })
                assertTrue(key === random.buffers.first())
            }

            assertArrayEquals(ByteArray(random.buffers.first().size), random.buffers.first())
            random.buffers.drop(1).forEach { assertArrayEquals(ByteArray(it.size), it) }
        }

    @Test
    fun `read access requires fresh authentication and clear revokes it`() =
        runBlocking {
            assertFreshAuthenticationRequired()

            manager.unlockAfterFreshAuthentication()
            val unlocked = manager.withUnlockedKey(ByteArray::clone)
            assertTrue(unlocked.any { it != 0.toByte() })
            unlocked.fill(0)

            manager.clear()
            assertFreshAuthenticationRequired()
        }

    @Test
    fun `fresh authentication expires after ttl`() =
        runBlocking {
            manager.unlockAfterFreshAuthentication(ttlMs = 20L)
            manager.withUnlockedKey { assertTrue(it.isNotEmpty()) }

            delay(100L)

            assertFreshAuthenticationRequired()
        }

    private suspend fun assertFreshAuthenticationRequired() {
        val failure = runCatching { manager.withUnlockedKey { Unit } }.exceptionOrNull()
        assertEquals(FreshAuthenticationRequiredException::class, failure!!::class)
    }

    private fun ByteArray.containsSubsequence(candidate: ByteArray): Boolean {
        if (candidate.isEmpty() || candidate.size > size) return false
        return indices
            .take(size - candidate.size + 1)
            .any { offset -> candidate.indices.all { this[offset + it] == candidate[it] } }
    }
}
