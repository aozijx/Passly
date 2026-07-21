package com.aozijx.passly.domain.repository.snapshot

import com.aozijx.passly.domain.model.history.VaultSnapshot

interface CommandSnapshotRepository {
    suspend fun insert(snapshot: VaultSnapshot)
    suspend fun deleteByEntryId(entryId: String)
}
