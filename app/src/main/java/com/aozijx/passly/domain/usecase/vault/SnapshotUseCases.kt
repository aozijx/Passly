package com.aozijx.passly.domain.usecase.vault

import com.aozijx.passly.domain.model.history.VaultSnapshot
import com.aozijx.passly.domain.repository.vault.SnapshotRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnapshotUseCases @Inject constructor(
    private val snapshotRepository: SnapshotRepository
) {
    fun getHistoryByEntryId(entryId: String): Flow<List<VaultSnapshot>> =
        snapshotRepository.getHistoryByEntryId(entryId)

    suspend fun insertHistory(history: VaultSnapshot) =
        snapshotRepository.insertHistory(history)
}
