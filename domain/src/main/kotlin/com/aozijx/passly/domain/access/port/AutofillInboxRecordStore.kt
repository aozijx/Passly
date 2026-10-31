package com.aozijx.passly.domain.access.port

import com.aozijx.passly.domain.autofill.model.AutofillInboxDraftId
import com.aozijx.passly.domain.autofill.model.AutofillInboxStoredRecord

interface AutofillInboxRecordStore {
    /** Returns false when the bounded queue cannot accept another record. */
    suspend fun enqueue(
        keyId: ByteArray,
        draftId: AutofillInboxDraftId,
        queuedAtMs: Long,
        encodedRecord: ByteArray,
    ): Boolean

    suspend fun pending(keyId: ByteArray): List<AutofillInboxStoredRecord>

    suspend fun remove(keyId: ByteArray, draftId: AutofillInboxDraftId)

    suspend fun quarantine(keyId: ByteArray, draftId: AutofillInboxDraftId)
}
