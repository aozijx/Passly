package com.aozijx.passly.domain.entry.model.draft

import com.aozijx.passly.domain.entry.model.EntryHeader
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntrySummary

data class EntryDraft(
    val draftId: String,
    val status: DraftStatus,
    val header: EntryHeader? = null,
    val summary: EntrySummary? = null,
    val secret: EntrySecret? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
