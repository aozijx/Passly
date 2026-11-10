package com.aozijx.passly.domain.entry.port

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId

/** Domain operations for discovering, creating, and removing related-password links. */
interface RelatedPasswordOperations {
    suspend fun candidates(sourceEntryId: EntryId): List<EntryListItem>

    suspend fun bind(
        sourceEntryId: EntryId,
        targetEntryId: EntryId,
    ): AppResult<Unit>

    suspend fun unlink(
        sourceEntryId: EntryId,
        targetEntryId: EntryId,
        linkId: EntryLinkId,
    ): AppResult<Unit>
}
