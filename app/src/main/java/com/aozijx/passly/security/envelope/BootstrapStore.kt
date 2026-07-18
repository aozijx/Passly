package com.aozijx.passly.security.envelope

import com.aozijx.passly.domain.model.envelope.EnvelopeType
import com.aozijx.passly.domain.model.envelope.KeyEnvelope

interface BootstrapStore {

    suspend fun save(envelope: KeyEnvelope)

    suspend fun load(type: EnvelopeType): KeyEnvelope?

    suspend fun loadAll(): List<KeyEnvelope>

    suspend fun delete(type: EnvelopeType)

    suspend fun saveVerificationTag(tag: ByteArray)

    suspend fun loadVerificationTag(): ByteArray?

    suspend fun loadBiometricState(): BiometricBootstrapState

    suspend fun prepareBiometricRotation(journal: BiometricRotationJournal)

    suspend fun commitBiometricRotation(
        envelope: KeyEnvelope,
        binding: BiometricBinding,
        obsoleteAlias: String?
    )

    suspend fun clearBiometricCleanupAlias(alias: String)

    suspend fun disableBiometric(activeAlias: String)

    suspend fun clearBiometricRotationJournal()

    suspend fun clear()
}

data class BiometricBinding(
    val activeAlias: String,
    val invalidateOnEnrollment: Boolean
)

enum class BiometricRotationPhase { NONE, PREPARED, COMMITTED }

data class BiometricRotationJournal(
    val phase: BiometricRotationPhase,
    val oldAlias: String?,
    val candidateAlias: String
)

data class BiometricBootstrapState(
    val binding: BiometricBinding?,
    val rotation: BiometricRotationJournal?,
    val cleanupAliases: Set<String>
)
