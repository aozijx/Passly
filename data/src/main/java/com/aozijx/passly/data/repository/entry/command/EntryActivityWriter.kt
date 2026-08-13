package com.aozijx.passly.data.repository.entry.command

import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.local.database.entity.EntryActivityEntity
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 活动记录协作器。
 *
 * 封装条目活动记录的写入，供各 Command Executor 在事务内调用。
 */
@Singleton
internal class EntryActivityWriter @Inject constructor() {

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
