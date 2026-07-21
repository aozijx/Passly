package com.aozijx.passly.domain.usecase.vault

import com.aozijx.passly.domain.model.activity.VaultActivity
import com.aozijx.passly.domain.repository.activity.QueryActivityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityQueryUseCases @Inject constructor(
    private val queryActivityRepository: QueryActivityRepository
) {
    fun observeByEntryId(entryId: String): Flow<List<VaultActivity>> =
        queryActivityRepository.observeByEntryId(entryId)

    fun observeAll(): Flow<List<VaultActivity>> =
        queryActivityRepository.observeAll()
}
