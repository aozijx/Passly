package com.aozijx.passly.security.autofill.inbox

import com.aozijx.passly.core.observability.ErrorCode
import com.aozijx.passly.core.observability.EventKey
import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.TraceScope
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.trace.TraceToken
import com.aozijx.passly.domain.access.model.AutofillInboxKeyMaterial
import com.aozijx.passly.domain.access.port.AutofillInboxBootstrapStore
import com.aozijx.passly.domain.access.port.AutofillInboxRecordStore
import com.aozijx.passly.domain.autofill.model.AutofillInboxEnqueueOutcome
import com.aozijx.passly.domain.autofill.model.AutofillInboxPendingDraft
import com.aozijx.passly.domain.autofill.model.AutofillInboxSource
import com.aozijx.passly.domain.autofill.model.AutofillInboxStoredRecord
import com.aozijx.passly.domain.autofill.model.AutofillSaveDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillInboxQueueWriterTest {
    private val keyMaterialCodec = AutofillInboxKeyMaterialCodec()
    private val recordCodec = AutofillInboxRecordCodec(keyMaterialCodec)
    private val dek = ByteArray(32) { (it + 7).toByte() }

    @Test
    fun queuedDraftIsEncryptedAndCanBeOpenedAfterUnlock() = runBlocking {
        val material = keyMaterialCodec.generateAndWrap(dek)
        val store = MemoryRecordStore()
        val writer = writer(material, store)

        assertEquals(
            AutofillInboxEnqueueOutcome.Queued,
            writer.enqueue(AutofillInboxSource.CREDENTIAL_MANAGER, draft()),
        )

        val stored = store.records.single()
        val decoded = keyMaterialCodec.withPrivateKey(material, dek) { privateKey ->
            recordCodec.open(material, privateKey, stored.encodedRecord)
        }
        assertEquals(AutofillInboxSource.CREDENTIAL_MANAGER, decoded.source)
        assertEquals(draft(), decoded.draft)
        assertFalse(stored.encodedRecord.toString(Charsets.ISO_8859_1).contains("correct horse"))
    }

    @Test
    fun lockedSaveWithoutProvisionedKeyIsRejected() = runBlocking {
        val store = MemoryRecordStore()
        val writer = writer(material = null, store = store)

        assertEquals(
            AutofillInboxEnqueueOutcome.NotProvisioned,
            writer.enqueue(AutofillInboxSource.LEGACY_SERVICE, draft()),
        )
        assertTrue(store.records.isEmpty())
    }

    @Test
    fun fullQueueDoesNotReportSaveAccepted() = runBlocking {
        val material = keyMaterialCodec.generateAndWrap(dek)
        val store = MemoryRecordStore(accept = false)
        val writer = writer(material, store)

        assertEquals(
            AutofillInboxEnqueueOutcome.Full,
            writer.enqueue(AutofillInboxSource.LEGACY_SERVICE, draft()),
        )
        assertTrue(store.records.isEmpty())
    }

    @Test
    fun cancellationFromStorageIsPropagated() = runBlocking {
        val material = keyMaterialCodec.generateAndWrap(dek)
        val store = MemoryRecordStore(failure = CancellationException("cancelled"))
        val writer = writer(material, store)

        try {
            writer.enqueue(AutofillInboxSource.LEGACY_SERVICE, draft())
            error("Expected cancellation")
        } catch (_: CancellationException) {
            assertTrue(true)
        }
    }

    private fun writer(material: AutofillInboxKeyMaterial?, store: MemoryRecordStore) =
        AutofillInboxQueueWriter(
            bootstrapStore = MemoryBootstrapStore(material),
            recordStore = store,
            recordCodec = recordCodec,
            logger = NoOpLogger,
        )

    private fun draft() = AutofillSaveDraft(
        packageName = "example.app",
        webDomain = "example.com",
        pageTitle = "Sign in",
        username = "person@example.com",
        password = "correct horse battery staple",
    )

    private class MemoryBootstrapStore(
        private val material: AutofillInboxKeyMaterial?,
    ) : AutofillInboxBootstrapStore {
        override suspend fun saveAutofillInboxKeyMaterial(material: AutofillInboxKeyMaterial) = Unit
        override suspend fun loadAutofillInboxKeyMaterial() = material
        override suspend fun clearAutofillInboxKeyMaterial() = Unit
    }

    private class MemoryRecordStore(
        private val accept: Boolean = true,
        private val failure: Exception? = null,
    ) : AutofillInboxRecordStore {
        val records = mutableListOf<AutofillInboxStoredRecord>()

        override suspend fun enqueue(
            keyId: ByteArray,
            draftId: com.aozijx.passly.domain.autofill.model.AutofillInboxDraftId,
            queuedAtMs: Long,
            encodedRecord: ByteArray,
        ): Boolean {
            failure?.let { throw it }
            if (!accept) return false
            records += AutofillInboxStoredRecord(draftId, queuedAtMs, encodedRecord.copyOf())
            return true
        }

        override suspend fun pending(keyId: ByteArray): List<AutofillInboxStoredRecord> = records.toList()
        override suspend fun remove(
            keyId: ByteArray,
            draftId: com.aozijx.passly.domain.autofill.model.AutofillInboxDraftId,
        ) = Unit
        override suspend fun quarantine(
            keyId: ByteArray,
            draftId: com.aozijx.passly.domain.autofill.model.AutofillInboxDraftId,
        ) = Unit
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
        ): T = error("Not used")
        override suspend fun <T> inSpan(
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ): T = error("Not used")
        override fun captureTraceToken(singleUse: Boolean): TraceToken = error("Not used")
        override suspend fun <T> resumeTrace(
            token: TraceToken,
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ): com.aozijx.passly.core.observability.TraceResumeResult<T> = error("Not used")
    }
}
