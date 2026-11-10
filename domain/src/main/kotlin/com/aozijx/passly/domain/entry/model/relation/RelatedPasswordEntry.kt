package com.aozijx.passly.domain.entry.model.relation

import com.aozijx.passly.domain.entry.model.EntryId

/** A password entry joined with the relation that links it to another entry. */
data class RelatedPasswordEntry(
    val linkId: EntryLinkId,
    val targetEntryId: EntryId,
    val title: String,
    val username: String,
)
