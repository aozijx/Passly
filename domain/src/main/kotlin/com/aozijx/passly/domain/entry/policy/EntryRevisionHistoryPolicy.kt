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
        val currentSnapshot = snapshots[current.metadata.id]
        val historicalSummaries = summaries.drop(1)
        val visibleByCurrent =
            historicalSummaries
                .filter { summary ->
                    val snapshot = snapshots[summary.metadata.id]
                    snapshot == null || currentSnapshot == null ||
                        EntryRevisionComparator
                            .compare(historical = snapshot, current = currentSnapshot)
                            .any { it.kind != RevisionDifferenceKind.UNCHANGED }
                }
        val visibleHistory =
            visibleByCurrent.filterIndexed { index, summary ->
                val nextOlder = visibleByCurrent.getOrNull(index + 1) ?: current
                val snapshot = snapshots[summary.metadata.id]
                val olderSnapshot = snapshots[nextOlder.metadata.id]
                nextOlder == current || snapshot == null || olderSnapshot == null ||
                    EntryRevisionComparator
                        .compare(historical = snapshot, current = olderSnapshot)
                        .any { it.kind != RevisionDifferenceKind.UNCHANGED }
            }
        return listOf(current) + visibleHistory
    }
}
