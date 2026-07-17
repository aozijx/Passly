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

    override suspend fun clear() {
        dataStore.updateData {
            it.toBuilder().clearEnvelopes().clearVerificationTag().build()
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
