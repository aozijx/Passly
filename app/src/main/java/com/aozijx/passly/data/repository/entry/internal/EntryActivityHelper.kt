package com.aozijx.passly.data.repository.entry.internal

import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.model.entity.EntryActivityEntity
import com.aozijx.passly.domain.model.activity.ActivityType
import com.aozijx.passly.domain.model.activity.EntryActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 活动记录协作器。
 *
 * 封装条目活动记录的写入，供各 Command Executor 在事务内调用。
 */
@Singleton
class EntryActivityHelper @Inject constructor() {

    /**
     * 记录一条活动。
     */
    suspend fun recordActivity(
        db: AppDatabase,
        entryId: String,
        activityType: ActivityType,
        now: Long
    ) {
        with(db) {
            entryActivityCommandDao().insertIdempotent(
                EntryActivity(entryId = entryId, activityType = activityType).toEntity(now)
            )
        }
    }

    internal companion object {

        private fun EntryActivity.toEntity(now: Long) =
            EntryActivityEntity(
                activityId = activityId,
                entryId = entryId,
                activityType = activityType,
                createdAt = now
            )
    }
}
