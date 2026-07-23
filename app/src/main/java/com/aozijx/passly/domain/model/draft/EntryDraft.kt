package com.aozijx.passly.domain.model.draft

import com.aozijx.passly.domain.model.entry.EntryHeader
import com.aozijx.passly.domain.model.entry.EntrySecret
import com.aozijx.passly.domain.model.entry.EntrySummary

data class EntryDraft(
    val draftId: String,
    val status: DraftStatus,
    val header: EntryHeader? = null,
    val summary: EntrySummary? = null,
    val secret: EntrySecret? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
