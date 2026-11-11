package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.history.EntryRevisionId
import com.aozijx.passly.domain.entry.model.history.EntryRevisionMetadata
import com.aozijx.passly.domain.entry.model.history.EntryRevisionSummary
import com.aozijx.passly.domain.entry.model.history.RedactedEntryRevision
import com.aozijx.passly.domain.entry.model.history.RevisionDifferenceKind
import com.aozijx.passly.domain.entry.model.history.RevisionFieldId

/** Applies vault history rules to revision metadata and the redacted snapshots that could be loaded. */
object EntryRevisionHistoryPolicy {
    fun summarize(
        metadata: List<EntryRevisionMetadata>,
        snapshots: Map<EntryRevisionId, RedactedEntryRevision>,
    ): List<EntryRevisionSummary> {
        val ordered = metadata.sortedByDescending { it.version.value }
        val summaries =
            ordered.mapIndexed { index, revision ->
                val olderRevision = ordered.getOrNull(index + 1)
                val revisionSnapshot = snapshots[revision.id]
                val olderSnapshot = olderRevision?.let { snapshots[it.id] }
                val differences =
                    if (revisionSnapshot == null || olderSnapshot == null) {
                        emptyList()
                    } else {
                        EntryRevisionComparator.compare(historical = revisionSnapshot, current = olderSnapshot)
                    }
                EntryRevisionSummary(
                    metadata = revision,
                    changedFields =
                        differences
                            .asSequence()
                            .filter { it.kind != RevisionDifferenceKind.UNCHANGED }
                            .map { it.field }
                            .toSet<RevisionFieldId>(),
                )
            }
        val current = summaries.firstOrNull() ?: return emptyList()
        val historicalSummaries = summaries.drop(1)
        val visibleHistory =
            historicalSummaries.filterIndexed { index, revision ->
                val nextOlder = historicalSummaries.getOrNull(index + 1) ?: current
                val revisionSnapshot = snapshots[revision.metadata.id]
                val olderSnapshot = snapshots[nextOlder.metadata.id]
                revisionSnapshot == null || olderSnapshot == null ||
                    EntryRevisionComparator
                        .compare(historical = revisionSnapshot, current = olderSnapshot)
                        .any { it.kind != RevisionDifferenceKind.UNCHANGED }
            }
        return listOf(current) + visibleHistory
    }
}
