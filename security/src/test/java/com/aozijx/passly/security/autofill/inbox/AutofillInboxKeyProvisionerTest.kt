package com.aozijx.passly.security.autofill.inbox

import com.aozijx.passly.core.observability.ErrorCode
import com.aozijx.passly.core.observability.EventKey
import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.TraceResumeResult
import com.aozijx.passly.core.observability.TraceScope
import com.aozijx.passly.core.observability.trace.TraceToken
import com.aozijx.passly.domain.access.model.AutofillInboxKeyMaterial
import com.aozijx.passly.domain.access.model.BiometricBinding
import com.aozijx.passly.domain.access.model.BiometricBootstrapState
import com.aozijx.passly.domain.access.model.BiometricRotationJournal
import com.aozijx.passly.domain.access.model.EnvelopeType
import com.aozijx.passly.domain.access.model.KeyEnvelope
import com.aozijx.passly.domain.access.port.AutofillInboxBootstrapStore
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import com.aozijx.passly.security.dek.DekManager
import com.aozijx.passly.security.dek.FieldKeyManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

class AutofillInboxKeyProvisionerTest {
    @Test
    fun provisionsOnceAndValidatesThePersistedKeyOnLaterUnlocks() = runBlocking {
        val bootstrap = MemoryBootstrapStore()
        val dekManager = DekManager(bootstrap, FieldKeyManager(), NoOpLogger).also {
            it.setDek(EnvelopeType.APP_PASSWORD, ByteArray(32) { 3 })
        }
        val provisioner = AutofillInboxKeyProvisioner(
            dekManager = dekManager,
            bootstrapStore = bootstrap,
            keyMaterialCodec = AutofillInboxKeyMaterialCodec(),
            logger = NoOpLogger,
        )

        provisioner.provisionAfterUnlock()
        val firstMaterial = bootstrap.inboxKeyMaterial
        assertNotNull(firstMaterial)
        provisioner.provisionAfterUnlock()

        assertSame(firstMaterial, bootstrap.inboxKeyMaterial)
        assertEquals(1, bootstrap.inboxSaveCount)
    }

    @Test
    fun malformedExistingKeyIsNotOverwritten() = runBlocking {
        val malformed = AutofillInboxKeyMaterial(
            formatVersion = 0,
            keyId = byteArrayOf(),
            publicKey = byteArrayOf(),
            publicKeySha256 = byteArrayOf(),
            privateKeyEnvelope = byteArrayOf(),
        )
        val bootstrap = MemoryBootstrapStore().apply { inboxKeyMaterial = malformed }
        val dekManager = DekManager(bootstrap, FieldKeyManager(), NoOpLogger).also {
            it.setDek(EnvelopeType.APP_PASSWORD, ByteArray(32) { 3 })
        }
        val provisioner = AutofillInboxKeyProvisioner(
            dekManager = dekManager,
            bootstrapStore = bootstrap,
            keyMaterialCodec = AutofillInboxKeyMaterialCodec(),
            logger = NoOpLogger,
        )

        provisioner.provisionAfterUnlock()

        assertSame(malformed, bootstrap.inboxKeyMaterial)
        assertEquals(0, bootstrap.inboxSaveCount)
    }

    @Test
    fun cancellationDuringBootstrapReadPropagates() = runBlocking {
        val bootstrap = MemoryBootstrapStore().apply {
            loadFailure = CancellationException("cancel provisioning")
        }
        val dekManager = DekManager(bootstrap, FieldKeyManager(), NoOpLogger).also {
            it.setDek(EnvelopeType.APP_PASSWORD, ByteArray(32) { 3 })
        }
        val provisioner = AutofillInboxKeyProvisioner(
            dekManager = dekManager,
            bootstrapStore = bootstrap,
            keyMaterialCodec = AutofillInboxKeyMaterialCodec(),
            logger = NoOpLogger,
        )

        assertThrows(CancellationException::class.java) { runBlocking { provisioner.provisionAfterUnlock() } }
        assertEquals(0, bootstrap.inboxSaveCount)
    }

    private class MemoryBootstrapStore : VaultBootstrapStore, AutofillInboxBootstrapStore {
        private var verificationTag: ByteArray? = null
        var inboxKeyMaterial: AutofillInboxKeyMaterial? = null
        var loadFailure: CancellationException? = null
        var inboxSaveCount: Int = 0
            private set

        override suspend fun save(envelope: KeyEnvelope) = Unit
        override suspend fun load(type: EnvelopeType): KeyEnvelope? = null
        override suspend fun loadAll(): List<KeyEnvelope> = emptyList()
        override suspend fun delete(type: EnvelopeType) = Unit
        override suspend fun saveVerificationTag(tag: ByteArray) {
            verificationTag = tag.clone()
        }
        override suspend fun loadVerificationTag(): ByteArray? = verificationTag?.clone()
        override suspend fun saveSensitiveKeyEnvelope(envelope: ByteArray) = Unit
        override suspend fun loadSensitiveKeyEnvelope(): ByteArray? = null
        override suspend fun saveAttachmentKeyEnvelope(envelope: ByteArray) = Unit
        override suspend fun loadAttachmentKeyEnvelope(): ByteArray? = null
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
            inboxKeyMaterial = null
        }
        override suspend fun saveAutofillInboxKeyMaterial(material: AutofillInboxKeyMaterial) {
            inboxKeyMaterial = material
            inboxSaveCount++
        }
        override suspend fun loadAutofillInboxKeyMaterial(): AutofillInboxKeyMaterial? {
            loadFailure?.let { throw it }
            return inboxKeyMaterial
        }
        override suspend fun clearAutofillInboxKeyMaterial() {
            inboxKeyMaterial = null
        }
    }

    private object NoOpLogger : Logger {
        override fun debug(event: EventKey, fields: LogFields) = Unit
        override fun info(event: EventKey, fields: LogFields) = Unit
        override fun degraded(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, fields: LogFields) = Unit
        override fun warn(
            event: EventKey,
            errorCode: ErrorCode,
            reason: ReasonCode,
            throwable: Throwable,
            fields: LogFields,
        ) = Unit
        override fun error(
            event: EventKey,
            errorCode: ErrorCode,
            reason: ReasonCode,
            throwable: Throwable,
            fields: LogFields,
        ) = Unit
        override fun fatal(
            event: EventKey,
            errorCode: ErrorCode,
            reason: ReasonCode,
            throwable: Throwable,
            fields: LogFields,
        ) = Unit
        override suspend fun <T> withRootTrace(
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ): T = block(TestTraceScope)
        override suspend fun <T> inSpan(
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ): T = block(TestTraceScope)
        override fun captureTraceToken(singleUse: Boolean): TraceToken = error("Not used in test")
        override suspend fun <T> resumeTrace(
            token: TraceToken,
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ): TraceResumeResult<T> = TraceResumeResult.Unavailable
    }

    private object TestTraceScope : TraceScope {
        override val traceId = TraceId("test_trace")
        override val spanId = SpanId("test_span")
        override val parentSpanId: SpanId? = null
    }
}
