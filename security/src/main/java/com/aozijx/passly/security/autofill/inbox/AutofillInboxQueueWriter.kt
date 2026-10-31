package com.aozijx.passly.security.autofill.inbox

import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.domain.access.port.AutofillInboxBootstrapStore
import com.aozijx.passly.domain.access.port.AutofillInboxRecordStore
import com.aozijx.passly.domain.autofill.port.AutofillInboxQueue
import com.aozijx.passly.domain.autofill.model.AutofillInboxEnqueueOutcome
import com.aozijx.passly.domain.autofill.model.AutofillInboxSource
import com.aozijx.passly.domain.autofill.model.AutofillSaveDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AutofillInboxQueueWriter @Inject constructor(
    private val bootstrapStore: AutofillInboxBootstrapStore,
    private val recordStore: AutofillInboxRecordStore,
    private val recordCodec: AutofillInboxRecordCodec,
    private val logger: Logger,
) : AutofillInboxQueue {
    override suspend fun enqueue(
        source: AutofillInboxSource,
        draft: AutofillSaveDraft,
    ): AutofillInboxEnqueueOutcome {
        return try {
            val material = bootstrapStore.loadAutofillInboxKeyMaterial()
                ?: return AutofillInboxEnqueueOutcome.NotProvisioned
            val nowMs = System.currentTimeMillis()
            val draftId = recordCodec.createDraftId()
            val encoded = withContext(Dispatchers.Default) {
                recordCodec.seal(
                    material = material,
                    draftId = draftId,
                    source = source,
                    createdAtMs = nowMs,
                    capturedAtMs = nowMs,
                    draft = draft,
                )
            }
            try {
                if (
                    recordStore.enqueue(
                        keyId = material.keyId,
                        draftId = draftId,
                        queuedAtMs = nowMs,
                        encodedRecord = encoded,
                    )
                ) {
                    AutofillInboxEnqueueOutcome.Queued
                } else {
                    AutofillInboxEnqueueOutcome.Full
                }
            } finally {
                encoded.fill(0)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            logger.log(
                LogStatements.Autofill.saveFailed(
                    throwable = IllegalStateException("Autofill Inbox enqueue failed"),
                    reason = Reasons.StorageFailure,
                ),
            )
            AutofillInboxEnqueueOutcome.Failed
        }
    }
}
