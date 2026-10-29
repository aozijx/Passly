package com.aozijx.passly.domain.autofill.model

data class AutofillInboxStoredRecord(
    val draftId: AutofillInboxDraftId,
    val queuedAtMs: Long,
    val encodedRecord: ByteArray,
)
