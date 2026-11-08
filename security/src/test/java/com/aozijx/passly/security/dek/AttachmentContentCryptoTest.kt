package com.aozijx.passly.security.dek

import com.aozijx.passly.core.error.boundary.CryptoException
import com.aozijx.passly.domain.access.model.AuthenticationCredentialSnapshot
import com.aozijx.passly.domain.access.model.BiometricBinding
import com.aozijx.passly.domain.access.model.BiometricBootstrapState
import com.aozijx.passly.domain.access.model.BiometricRotationJournal
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.KeyEnvelope
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import com.aozijx.passly.testing.testLogger
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AttachmentContentCryptoTest {
    private lateinit var dekManager: DekManager
    private lateinit var store: InMemoryVaultBootstrapStore
    private lateinit var crypto: AttachmentContentCrypto

    @Before
    fun setUp() =
        runBlocking {
            store = InMemoryVaultBootstrapStore()
            dekManager = DekManager(store, FieldKeyManager(), testLogger())
            check(dekManager.setDek(EnvelopeType.APP_PASSWORD, ByteArray(32) { it.toByte() }) == DekUnlockResult.Success)
            crypto = AttachmentContentCrypto(AttachmentDataKeyManager(store, dekManager, CapturingSecureRandom()))
        }

    @Test
    fun `arbitrary bytes use stable keyed id and round trip`() =
        runBlocking {
            val content = byteArrayOf(0, 1, -1, 13, 10, 0, 42)
            val contentId = crypto.contentId(content)
            val rawSha256 = MessageDigest.getInstance("SHA-256").digest(content).toHex()

            assertNotEquals(rawSha256, contentId)
            assertEquals64Hex(contentId)
            assertTrue(crypto.verifyContentId(content, contentId))
            val encrypted = crypto.encrypt(content, contentId)
            assertFalse(encrypted.contentEquals(content))
            assertArrayEquals(content, crypto.decrypt(encrypted, contentId))
        }

    @Test
    fun `resource id is authenticated and locked session cannot access content`() =
        runBlocking {
            val content = "# markdown with image".toByteArray()
            val id = crypto.contentId(content)
            val encrypted = crypto.encrypt(content, id)
            val tagFailure = runCatching { crypto.decrypt(encrypted, "a".repeat(64)) }.exceptionOrNull()
            assertEquals(CryptoException.TagVerificationFailed::class, tagFailure!!::class)

            dekManager.lock()
            val lockedFailure = runCatching { crypto.contentId(content) }.exceptionOrNull()
            assertEquals(IllegalStateException::class, lockedFailure!!::class)
        }

    @Test
    fun `new attachment key and nonce are wiped when nonce generation fails`() =
        runBlocking {
            val injectedFailure = IllegalStateException("nonce generation failed")
            val random = CapturingSecureRandom(failingCall = 2, failure = injectedFailure)
            val manager = AttachmentDataKeyManager(store, dekManager, random)

            val failure =
                withContext(NonCancellable) {
                    runCatching { manager.withKey { Unit } }.exceptionOrNull()
                }

            assertSame(injectedFailure, failure)
            assertEquals(2, random.buffers.size)
            random.buffers.forEach { assertArrayEquals(ByteArray(it.size), it) }
        }

    @Test
    fun `new attachment key is wiped when random generation fails partway through`() =
        runBlocking {
            val injectedFailure = IllegalStateException("key generation failed")
            val random = CapturingSecureRandom(failingCall = 1, failure = injectedFailure)
            val manager = AttachmentDataKeyManager(store, dekManager, random)

            val failure =
                withContext(NonCancellable) {
                    runCatching { manager.withKey { Unit } }.exceptionOrNull()
                }

            assertSame(injectedFailure, failure)
            assertEquals(1, random.buffers.size)
            assertArrayEquals(ByteArray(random.buffers.first().size), random.buffers.first())
        }

    @Test
    fun `new attachment key is wiped when envelope persistence fails`() =
        runBlocking {
            val random = CapturingSecureRandom()
            val manager = AttachmentDataKeyManager(store, dekManager, random)
            val injectedFailure = IllegalStateException("attachment envelope save failed")
            store.attachmentKeySaveFailure = injectedFailure

            val failure =
                withContext(NonCancellable) {
                    runCatching { manager.withKey { Unit } }.exceptionOrNull()
                }

            assertSame(injectedFailure, failure)
            assertEquals(2, random.buffers.size)
            random.buffers.forEach { assertArrayEquals(ByteArray(it.size), it) }
        }

    @Test
    fun `new attachment key is wiped when envelope persistence is cancelled or fatal`() =
        runBlocking {
            listOf(CancellationException("save cancelled"), AssertionError("fatal save failure")).forEach { injectedFailure ->
                val random = CapturingSecureRandom()
                val manager = AttachmentDataKeyManager(store, dekManager, random)
                store.attachmentKeySaveFailure = injectedFailure

                val observed =
                    withContext(NonCancellable) {
                        runCatching { manager.withKey { Unit } }.exceptionOrNull()
                    }

                assertSame(injectedFailure, observed)
                assertEquals(2, random.buffers.size)
                random.buffers.forEach { assertArrayEquals(ByteArray(it.size), it) }
            }
            store.attachmentKeySaveFailure = null
        }

    @Test
    fun `new attachment key is wiped after successful caller handoff`() =
        runBlocking {
            val random = CapturingSecureRandom()
            val manager = AttachmentDataKeyManager(store, dekManager, random)

            manager.withKey { key ->
                assertTrue(key.any { it != 0.toByte() })
                assertTrue(key === random.buffers.first())
            }

            assertArrayEquals(ByteArray(random.buffers.first().size), random.buffers.first())
            random.buffers.drop(1).forEach { assertArrayEquals(ByteArray(it.size), it) }
        }

    private fun assertEquals64Hex(value: String) {
        assertTrue(value.matches(Regex("[a-f0-9]{64}")))
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    private class InMemoryVaultBootstrapStore : VaultBootstrapStore {
        private val envelopes = linkedMapOf<EnvelopeType, KeyEnvelope>()
        private var verificationTag: ByteArray? = null
        private var attachmentEnvelope: ByteArray? = null
        var attachmentKeySaveFailure: Throwable? = null

        override suspend fun save(envelope: KeyEnvelope) {
            envelopes[envelope.type] = envelope
        }

        override suspend fun load(type: EnvelopeType): KeyEnvelope? = envelopes[type]

        override suspend fun loadAll(): List<KeyEnvelope> = envelopes.values.toList()

        override suspend fun loadAuthenticationCredentialSnapshot() = AuthenticationCredentialSnapshot()

        override suspend fun delete(type: EnvelopeType) {
            envelopes.remove(type)
        }

        override suspend fun saveVerificationTag(tag: ByteArray) {
            verificationTag = tag.clone()
        }

        override suspend fun loadVerificationTag(): ByteArray? = verificationTag?.clone()

        override suspend fun saveSensitiveKeyEnvelope(envelope: ByteArray) = Unit

        override suspend fun loadSensitiveKeyEnvelope(): ByteArray? = null

        override suspend fun saveAttachmentKeyEnvelope(envelope: ByteArray) {
            attachmentKeySaveFailure?.let { throw it }
            attachmentEnvelope = envelope.clone()
        }

        override suspend fun loadAttachmentKeyEnvelope(): ByteArray? = attachmentEnvelope?.clone()

        override suspend fun loadBiometricState() = BiometricBootstrapState(null, null, emptySet())

        override suspend fun prepareBiometricRotation(journal: BiometricRotationJournal) = Unit

        override suspend fun commitBiometricRotation(
            envelope: KeyEnvelope,
            binding: BiometricBinding,
            obsoleteAlias: String?,
        ) = Unit

        override suspend fun clearBiometricCleanupAlias(alias: String) = Unit

        override suspend fun disableBiometric(activeAlias: String) = Unit

        override suspend fun clearBiometricRotationJournal() = Unit
    }
}
