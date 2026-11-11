package com.aozijx.passly.domain.entry.model.history

/** A revision annotated with the fields changed by the operation that created it. */
data class EntryRevisionSummary(
    val metadata: EntryRevisionMetadata,
    val changedFields: Set<RevisionFieldId>,
)

/** Separates the current revision used to annotate its activity from history rows shown to the user. */
sealed interface EntryRevisionHistory {
    data object Empty : EntryRevisionHistory

    data class Available(
        val latest: EntryRevisionSummary,
        val previous: List<EntryRevisionSummary>,
    ) : EntryRevisionHistory
}
