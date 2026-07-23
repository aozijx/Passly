package com.aozijx.passly.domain.usecase.detail

import com.aozijx.passly.domain.model.activity.EntryActivity
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.repository.entry.EntryQueryRepository
import com.aozijx.passly.domain.usecase.vault.ActivityQueryUseCases
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DetailQueryUseCases @Inject constructor(
    private val entryQueryRepository: EntryQueryRepository,
    private val activityQueryUseCases: ActivityQueryUseCases
) {

    suspend fun getById(entryId: String): VaultEntry? = entryQueryRepository.getById(entryId)

    fun getActivityByEntryId(entryId: String): Flow<List<EntryActivity>> =
        activityQueryUseCases.observeByEntryId(entryId)
}
