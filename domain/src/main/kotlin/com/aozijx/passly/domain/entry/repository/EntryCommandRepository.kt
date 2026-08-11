package com.aozijx.passly.domain.entry.repository

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.entry.model.EntryChanges
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.VaultEntry

interface EntryCommandRepository {
    suspend fun createEntry(entry: VaultEntry): AppResult<EntryId>
    suspend fun updateEntry(
        id: String,
        expectedVersion: Int,
        changes: EntryChanges
    ): AppResult<Unit>

    suspend fun moveToTrash(id: String, expectedVersion: Int): AppResult<Unit>
    suspend fun restoreEntry(id: String, expectedVersion: Int): AppResult<Unit>
    suspend fun deletePermanently(id: String, expectedVersion: Int): AppResult<Unit>
    suspend fun emptyTrash(): AppResult<Int>
}
