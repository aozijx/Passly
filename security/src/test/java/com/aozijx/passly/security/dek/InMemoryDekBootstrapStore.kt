package com.aozijx.passly.security.dek

import com.aozijx.passly.domain.access.model.AuthenticationCredentialSnapshot
import com.aozijx.passly.domain.access.model.BiometricBinding
import com.aozijx.passly.domain.access.model.BiometricBootstrapState
import com.aozijx.passly.domain.access.model.BiometricRotationJournal
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.KeyEnvelope
import com.aozijx.passly.domain.access.port.VaultBootstrapStore

/** Shared in-memory bootstrap store for DEK and wrapped data-key tests. */
internal class InMemoryDekBootstrapStore : VaultBootstrapStore {
    private val envelopes = linkedMapOf<EnvelopeType, KeyEnvelope>()
    private var verificationTag: ByteArray? = null
    private var attachmentKeyEnvelope: ByteArray? = null

    var sensitiveKeyEnvelope: ByteArray? = null
        private set

    var sensitiveKeySaveFailure: Throwable? = null
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

    override suspend fun saveSensitiveKeyEnvelope(envelope: ByteArray) {
        sensitiveKeySaveFailure?.let { throw it }
        sensitiveKeyEnvelope = envelope.clone()
    }

    override suspend fun loadSensitiveKeyEnvelope(): ByteArray? = sensitiveKeyEnvelope?.clone()

    override suspend fun saveAttachmentKeyEnvelope(envelope: ByteArray) {
        attachmentKeySaveFailure?.let { throw it }
        attachmentKeyEnvelope = envelope.clone()
    }

    override suspend fun loadAttachmentKeyEnvelope(): ByteArray? = attachmentKeyEnvelope?.clone()

    override suspend fun loadBiometricState() =
        BiometricBootstrapState(
            binding = null,
            rotation = null,
            cleanupAliases = emptySet(),
        )

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
