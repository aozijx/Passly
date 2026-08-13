package com.aozijx.passly.domain.entry.port

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.entry.model.EntryUpdate
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.Entry

interface EntryCommandRepository {
    suspend fun createEntry(entry: Entry): AppResult<EntryId>
    suspend fun updateEntry(
        id: EntryId,
        expectedVersion: EntryVersion,
        changes: EntryUpdate
    ): AppResult<Unit>

    suspend fun moveToTrash(id: EntryId, expectedVersion: EntryVersion): AppResult<Unit>
    suspend fun restoreEntry(id: EntryId, expectedVersion: EntryVersion): AppResult<Unit>
    suspend fun deletePermanently(id: EntryId, expectedVersion: EntryVersion): AppResult<Unit>
    suspend fun emptyTrash(): AppResult<Int>
}
