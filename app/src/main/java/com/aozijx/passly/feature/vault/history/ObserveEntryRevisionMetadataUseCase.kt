package com.aozijx.passly.feature.vault.history

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.history.EntryRevisionMetadata
import com.aozijx.passly.domain.entry.port.EntryRevisionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Ensures the one-time history baseline exists before the page starts its metadata subscription. */
internal class ObserveEntryRevisionMetadataUseCase
    @Inject
    constructor(
        private val revisionRepository: EntryRevisionRepository,
    ) {
        suspend operator fun invoke(entryId: EntryId): AppResult<Flow<List<EntryRevisionMetadata>>> =
            when (val initialization = revisionRepository.initializeHistory()) {
                is AppResult.Failure -> AppResult.Failure(initialization.error)
                is AppResult.Success -> AppResult.Success(revisionRepository.observeMetadata(entryId))
            }
    }
