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

    suspend fun clear()
}
