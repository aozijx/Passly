package com.aozijx.passly.domain.usecase.vault

import com.aozijx.passly.domain.model.history.VaultSnapshot
import com.aozijx.passly.domain.repository.snapshot.CommandSnapshotRepository
import com.aozijx.passly.domain.repository.snapshot.QuerySnapshotRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnapshotUseCases @Inject constructor(
    private val querySnapshotRepository: QuerySnapshotRepository,
    private val commandSnapshotRepository: CommandSnapshotRepository
) {
    fun observeByEntryId(entryId: String): Flow<List<VaultSnapshot>> =
        querySnapshotRepository.observeByEntryId(entryId)

    suspend fun insert(snapshot: VaultSnapshot) =
        commandSnapshotRepository.insert(snapshot)
}
