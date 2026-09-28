package com.aozijx.passly.feature.vault.history

import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.port.EntryRevisionRepository
import javax.inject.Inject

internal class ObserveEntryRevisionsUseCase @Inject constructor(
    private val repository: EntryRevisionRepository,
) {
    operator fun invoke(entryId: EntryId) = repository.observeMetadata(entryId)
}
