package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.port.EntryLinkRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import javax.inject.Inject

internal class DetailRelatedEntryLoader @Inject constructor(
    private val entryQueryRepository: EntryQueryRepository,
    private val entryLinkRepository: EntryLinkRepository,
) {
    suspend fun loadFor(entry: Entry): List<Entry> {
        val relatedIds = DetailRelatedEntryIds.resolve(
            entryId = entry.id,
            entryType = entry.type,
            links = entryLinkRepository.getAll(),
        )
        return relatedIds.mapNotNull { relatedId ->
            entryQueryRepository.getById(relatedId)
        }
    }
}
