package com.aozijx.passly.domain.usecase.vault

import com.aozijx.passly.domain.model.activity.EntryActivity
import com.aozijx.passly.domain.repository.activity.ActivityQueryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityQueryUseCases @Inject constructor(
    private val activityQueryRepository: ActivityQueryRepository
) {
    fun observeByEntryId(entryId: String): Flow<List<EntryActivity>> =
        activityQueryRepository.observeByEntryId(entryId)

    fun observeAll(): Flow<List<EntryActivity>> =
        activityQueryRepository.observeAll()
}
