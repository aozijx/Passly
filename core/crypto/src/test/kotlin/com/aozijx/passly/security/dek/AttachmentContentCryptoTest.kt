package com.aozijx.passly.security.dek

import com.aozijx.passly.core.telemetry.TelemetryReporter
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.KeyEnvelope
import com.aozijx.passly.domain.access.model.BiometricBinding
import com.aozijx.passly.domain.access.model.BiometricBootstrapState
import com.aozijx.passly.domain.access.model.BiometricRotationJournal
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.MessageDigest

class AttachmentContentCryptoTest {
    private lateinit var dekManager: DekManager
    private lateinit var crypto: AttachmentContentCrypto

    @Before
    fun setUp() = runBlocking {
        val store = InMemoryVaultBootstrapStore()
        dekManager = DekManager(store, FieldKeyManager(), TelemetryReporter { })
        check(dekManager.setDek(EnvelopeType.APP_PASSWORD, ByteArray(32) { it.toByte() }) == DekUnlockResult.Success)
        crypto = AttachmentContentCrypto(AttachmentDataKeyManager(store, dekManager))
    }

    @Test
    fun `arbitrary bytes use stable keyed id and round trip`() = runBlocking {
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
    fun `resource id is authenticated and locked session cannot access content`() = runBlocking {
        val content = "# markdown with image".toByteArray()
        val id = crypto.contentId(content)
        val encrypted = crypto.encrypt(content, id)
        assertTrue(runCatching { crypto.decrypt(encrypted, "a".repeat(64)) }.isFailure)

        dekManager.lock()
        assertTrue(runCatching { crypto.contentId(content) }.isFailure)
    }

    private fun assertEquals64Hex(value: String) {
        assertTrue(value.matches(Regex("[a-f0-9]{64}")))
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    private class InMemoryVaultBootstrapStore : VaultBootstrapStore {
        private val envelopes = linkedMapOf<EnvelopeType, KeyEnvelope>()
        private var verificationTag: ByteArray? = null
        private var attachmentEnvelope: ByteArray? = null

        override suspend fun save(envelope: KeyEnvelope) { envelopes[envelope.type] = envelope }
        override suspend fun load(type: EnvelopeType): KeyEnvelope? = envelopes[type]
        override suspend fun loadAll(): List<KeyEnvelope> = envelopes.values.toList()
        override suspend fun delete(type: EnvelopeType) { envelopes.remove(type) }
        override suspend fun saveVerificationTag(tag: ByteArray) { verificationTag = tag.clone() }
        override suspend fun loadVerificationTag(): ByteArray? = verificationTag?.clone()
        override suspend fun saveSensitiveKeyEnvelope(envelope: ByteArray) = Unit
        override suspend fun loadSensitiveKeyEnvelope(): ByteArray? = null
        override suspend fun saveAttachmentKeyEnvelope(envelope: ByteArray) {
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
        override suspend fun clear() {
            attachmentEnvelope?.fill(0)
            attachmentEnvelope = null
        }
    }
}
