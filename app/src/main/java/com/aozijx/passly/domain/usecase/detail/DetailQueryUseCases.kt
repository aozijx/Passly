package com.aozijx.passly.domain.usecase.detail

import com.aozijx.passly.domain.model.activity.EntryActivity
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.repository.entry.QueryRepository
import com.aozijx.passly.domain.usecase.vault.ActivityQueryUseCases
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DetailQueryUseCases @Inject constructor(
    private val queryRepository: QueryRepository,
    private val activityQueryUseCases: ActivityQueryUseCases
) {

    suspend fun getById(entryId: String): VaultEntry? = queryRepository.getById(entryId)

    fun getActivityByEntryId(entryId: String): Flow<List<EntryActivity>> =
        activityQueryUseCases.observeByEntryId(entryId)
}
