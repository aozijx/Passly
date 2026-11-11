package com.aozijx.passly.domain.entry.model.history

/** A revision annotated with the fields changed by the operation that created it. */
data class EntryRevisionSummary(
    val metadata: EntryRevisionMetadata,
    val changedFields: Set<RevisionFieldId>,
)
