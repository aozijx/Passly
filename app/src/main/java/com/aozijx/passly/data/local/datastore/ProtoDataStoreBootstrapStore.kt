package com.aozijx.passly.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.aozijx.passly.data.crypto.proto.BootstrapData
import com.aozijx.passly.data.crypto.proto.EnvelopeEntry
import com.aozijx.passly.domain.model.envelope.EnvelopeType
import com.aozijx.passly.domain.model.envelope.KdfAlgorithm
import com.aozijx.passly.domain.model.envelope.KeyEnvelope
import com.aozijx.passly.security.envelope.BootstrapStore
import com.aozijx.passly.security.envelope.BiometricBinding
import com.aozijx.passly.security.envelope.BiometricBootstrapState
import com.aozijx.passly.security.envelope.BiometricRotationJournal
import com.aozijx.passly.security.envelope.BiometricRotationPhase
import com.aozijx.passly.data.crypto.proto.BiometricBinding as BiometricBindingProto
import com.aozijx.passly.data.crypto.proto.BiometricRotationJournal as BiometricRotationJournalProto
import com.google.protobuf.ByteString
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.bootstrapDataStore: DataStore<BootstrapData> by dataStore(
    fileName = "bootstrap.pb",
    serializer = BootstrapSerializer
)

@Singleton
class ProtoDataStoreBootstrapStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) : BootstrapStore {

    private val dataStore: DataStore<BootstrapData>
        get() = context.bootstrapDataStore

    override suspend fun save(envelope: KeyEnvelope) {
        dataStore.updateData { current ->
            val builder = current.toBuilder()
            val index = current.envelopesList.indexOfFirst {
                it.type == envelope.type.value
            }
            val entry = envelope.toProto()
            if (index >= 0) {
                builder.setEnvelopes(index, entry)
            } else {
                builder.addEnvelopes(entry)
            }
            builder.build()
        }
    }

    override suspend fun load(type: EnvelopeType): KeyEnvelope? {
        val data = dataStore.data.first()
        return data.envelopesList
            .find { it.type == type.value }
            ?.toDomain()
    }

    override suspend fun loadAll(): List<KeyEnvelope> {
        val data = dataStore.data.first()
        return data.envelopesList.map { it.toDomain() }
    }

    override suspend fun delete(type: EnvelopeType) {
        dataStore.updateData { current ->
            val builder = current.toBuilder()
            val index = current.envelopesList.indexOfFirst {
                it.type == type.value
            }
            if (index >= 0) {
                builder.removeEnvelopes(index)
            }
            builder.build()
        }
    }

    override suspend fun saveVerificationTag(tag: ByteArray) {
        dataStore.updateData {
            it.toBuilder().setVerificationTag(ByteString.copyFrom(tag)).build()
        }
    }

    override suspend fun loadVerificationTag(): ByteArray? {
        val tag = dataStore.data.first().verificationTag
        return if (tag.isEmpty) null else tag.toByteArray()
    }

    override suspend fun loadBiometricState(): BiometricBootstrapState {
        val data = dataStore.data.first()
        val binding = data.takeIf { it.hasBiometricBinding() }
            ?.biometricBinding
            ?.takeIf { it.activeAlias.isNotBlank() }
            ?.let { BiometricBinding(it.activeAlias, it.invalidateOnEnrollment) }
        val rotation = data.takeIf { it.hasBiometricRotation() }
            ?.biometricRotation
            ?.takeIf { it.phase != BiometricRotationJournalProto.Phase.NONE }
            ?.let {
                BiometricRotationJournal(
                    phase = when (it.phase) {
                        BiometricRotationJournalProto.Phase.PREPARED -> BiometricRotationPhase.PREPARED
                        BiometricRotationJournalProto.Phase.COMMITTED -> BiometricRotationPhase.COMMITTED
                        else -> BiometricRotationPhase.NONE
                    },
                    oldAlias = it.oldAlias.takeIf(String::isNotBlank),
                    candidateAlias = it.candidateAlias
                )
            }
        return BiometricBootstrapState(binding, rotation, data.biometricCleanupAliasesList.toSet())
    }

    override suspend fun prepareBiometricRotation(journal: BiometricRotationJournal) {
        dataStore.updateData { current ->
            current.toBuilder().setBiometricRotation(journal.toProto()).build()
        }
    }

    override suspend fun commitBiometricRotation(
        envelope: KeyEnvelope,
        binding: BiometricBinding,
        obsoleteAlias: String?
    ) {
        dataStore.updateData { current ->
            val builder = current.toBuilder()
            val index = current.envelopesList.indexOfFirst { it.type == envelope.type.value }
            if (index >= 0) builder.setEnvelopes(index, envelope.toProto())
            else builder.addEnvelopes(envelope.toProto())
            builder.biometricBinding = BiometricBindingProto.newBuilder()
                .setActiveAlias(binding.activeAlias)
                .setInvalidateOnEnrollment(binding.invalidateOnEnrollment)
                .build()
            obsoleteAlias?.takeIf(String::isNotBlank)?.let { alias ->
                if (alias !in current.biometricCleanupAliasesList) builder.addBiometricCleanupAliases(alias)
            }
            builder.biometricRotation = current.biometricRotation.toBuilder()
                .setPhase(BiometricRotationJournalProto.Phase.COMMITTED)
                .build()
            builder.build()
        }
    }

    override suspend fun clearBiometricCleanupAlias(alias: String) {
        dataStore.updateData { current ->
            current.toBuilder()
                .clearBiometricCleanupAliases()
                .addAllBiometricCleanupAliases(current.biometricCleanupAliasesList.filterNot { it == alias })
                .build()
        }
    }

    override suspend fun disableBiometric(activeAlias: String) {
        dataStore.updateData { current ->
            val builder = current.toBuilder()
            val envelopeIndex = current.envelopesList.indexOfFirst {
                it.type == EnvelopeType.BIOMETRIC.value
            }
            if (envelopeIndex >= 0) builder.removeEnvelopes(envelopeIndex)
            builder.clearBiometricBinding()
            builder.clearBiometricRotation()
            if (activeAlias.isNotBlank() && activeAlias !in current.biometricCleanupAliasesList) {
                builder.addBiometricCleanupAliases(activeAlias)
            }
            builder.build()
        }
    }

    override suspend fun clearBiometricRotationJournal() {
        dataStore.updateData { it.toBuilder().clearBiometricRotation().build() }
    }

    override suspend fun clear() {
        dataStore.updateData {
            it.toBuilder()
                .clearEnvelopes()
                .clearVerificationTag()
                .clearBiometricBinding()
                .clearBiometricRotation()
                .clearBiometricCleanupAliases()
                .build()
        }
    }

    private fun KeyEnvelope.toProto(): EnvelopeEntry {
        return EnvelopeEntry.newBuilder()
            .setType(type.value)
            .setCiphertext(ByteString.copyFrom(ciphertext))
            .setIv(ByteString.copyFrom(iv))
            .setSalt(ByteString.copyFrom(salt))
            .setAlgorithm(algorithm.value)
            .setVersion(version)
            .build()
    }

    private fun BiometricRotationJournal.toProto(): BiometricRotationJournalProto =
        BiometricRotationJournalProto.newBuilder()
            .setPhase(
                when (phase) {
                    BiometricRotationPhase.NONE -> BiometricRotationJournalProto.Phase.NONE
                    BiometricRotationPhase.PREPARED -> BiometricRotationJournalProto.Phase.PREPARED
                    BiometricRotationPhase.COMMITTED -> BiometricRotationJournalProto.Phase.COMMITTED
                }
            )
            .setOldAlias(oldAlias.orEmpty())
            .setCandidateAlias(candidateAlias)
            .build()

    private fun EnvelopeEntry.toDomain(): KeyEnvelope {
        return KeyEnvelope(
            type = EnvelopeType(type),
            ciphertext = ciphertext.toByteArray(),
            iv = iv.toByteArray(),
            salt = salt.toByteArray(),
            algorithm = KdfAlgorithm(algorithm),
            version = version
        )
    }
}
