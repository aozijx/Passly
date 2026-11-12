package com.aozijx.passly.feature.vault.history

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.history.EntryRevisionMetadata
import com.aozijx.passly.domain.entry.model.history.EntryRevisionSummary
import com.aozijx.passly.domain.entry.policy.EntryRevisionHistoryPolicy
import com.aozijx.passly.domain.entry.port.EntryRevisionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Ensures the one-time history baseline exists before the page starts its metadata subscription. */
internal class ObserveEntryRevisionMetadataUseCase
    @Inject
    constructor(
        private val revisionRepository: EntryRevisionRepository,
    ) {
        suspend operator fun invoke(entryId: EntryId): AppResult<Flow<List<EntryRevisionSummary>>> =
            when (val initialization = revisionRepository.initializeHistory()) {
                is AppResult.Failure -> {
                    AppResult.Failure(initialization.error)
                }

                is AppResult.Success -> {
                    AppResult.Success(
                        revisionRepository.observeMetadata(entryId).map { revisions ->
                            summarizeLoadedRevisions(entryId, revisions)
                        },
                    )
                }
            }

        private suspend fun summarizeLoadedRevisions(
            entryId: EntryId,
            revisions: List<EntryRevisionMetadata>,
        ): List<EntryRevisionSummary> {
            val snapshots =
                revisions
                    .mapNotNull { revision ->
                        when (val result = revisionRepository.loadRedacted(entryId, revision.id)) {
                            is AppResult.Failure -> null
                            is AppResult.Success -> revision.id to result.data
                        }
                    }.toMap()
            return EntryRevisionHistoryPolicy.summarize(revisions, snapshots)
        }
    }
