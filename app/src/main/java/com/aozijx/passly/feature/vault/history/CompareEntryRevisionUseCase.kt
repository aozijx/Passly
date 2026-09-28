package com.aozijx.passly.feature.vault.history

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.history.EntryRevisionDifference
import com.aozijx.passly.domain.entry.model.history.EntryRevisionId
import com.aozijx.passly.domain.entry.policy.EntryRevisionComparator
import com.aozijx.passly.domain.entry.port.EntryRevisionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

internal class CompareEntryRevisionUseCase @Inject constructor(
    private val repository: EntryRevisionRepository,
) {
    suspend operator fun invoke(
        entryId: EntryId,
        revisionId: EntryRevisionId,
    ): RevisionOperationResult<List<EntryRevisionDifference>> {
        val latest = repository.observeMetadata(entryId).first().maxByOrNull { it.version.value }
            ?: return RevisionOperationResult.Missing
        val historical = repository.loadRedacted(entryId, revisionId)
        if (historical is AppResult.Failure) return historical.toRevisionOperationResult()
        val current = repository.loadRedacted(entryId, latest.id)
        if (current is AppResult.Failure) return current.toRevisionOperationResult()
        return RevisionOperationResult.Succeeded(
            EntryRevisionComparator.compare(
                historical = (historical as AppResult.Success).data,
                current = (current as AppResult.Success).data,
            ),
        )
    }
}
