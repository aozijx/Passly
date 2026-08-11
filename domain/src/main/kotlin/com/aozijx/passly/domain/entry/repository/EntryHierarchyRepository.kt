package com.aozijx.passly.domain.entry.repository

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.entry.model.EntryAggregate

/**
 * Manages ACCOUNT → credential ownership without merging credential payloads.
 */
interface EntryHierarchyRepository {
    suspend fun assignToAccount(
        entryId: String,
        expectedVersion: Int,
        accountEntryId: String?
    ): AppResult<Unit>

    suspend fun getChildren(accountEntryId: String): List<EntryAggregate>
}
