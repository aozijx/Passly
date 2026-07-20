package com.aozijx.passly.domain.usecase.vault

import com.aozijx.passly.domain.model.history.VaultSnapshot
import com.aozijx.passly.domain.repository.snapshot.SnapshotRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnapshotUseCases @Inject constructor(
    private val snapshotRepository: SnapshotRepository
) {
    fun observeByEntryId(entryId: String): Flow<List<VaultSnapshot>> =
        snapshotRepository.observeByEntryId(entryId)

    suspend fun insert(snapshot: VaultSnapshot) =
        snapshotRepository.insert(snapshot)
}
