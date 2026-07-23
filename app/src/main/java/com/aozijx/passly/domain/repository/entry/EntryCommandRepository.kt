package com.aozijx.passly.domain.repository.entry

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.entry.EntryChanges
import com.aozijx.passly.domain.model.entry.EntryId
import com.aozijx.passly.domain.model.entry.VaultEntry

interface EntryCommandRepository {
    suspend fun createEntry(entry: VaultEntry): AppResult<EntryId>
    suspend fun updateEntry(
        id: String,
        expectedVersion: Int,
        changes: EntryChanges
    ): AppResult<Unit>

    suspend fun moveToTrash(id: String, expectedVersion: Int): AppResult<Unit>
    suspend fun restoreEntry(id: String, expectedVersion: Int): AppResult<Unit>
}
