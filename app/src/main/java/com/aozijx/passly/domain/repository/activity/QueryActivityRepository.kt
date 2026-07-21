package com.aozijx.passly.domain.repository.activity

import com.aozijx.passly.domain.model.activity.ActivityType
import com.aozijx.passly.domain.model.activity.VaultActivity
import kotlinx.coroutines.flow.Flow

interface QueryActivityRepository {
    fun observeByEntryId(entryId: String): Flow<List<VaultActivity>>
    fun observeAll(): Flow<List<VaultActivity>>
    fun observeByType(activityType: ActivityType): Flow<List<VaultActivity>>
}
