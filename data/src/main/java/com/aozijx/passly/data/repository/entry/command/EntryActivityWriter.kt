package com.aozijx.passly.data.repository.entry.command

import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.mapper.activity.toEntity
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.activity.EntryActivity
import com.aozijx.passly.domain.entry.model.activity.EntryActivityContext
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
        now: Long,
        context: EntryActivityContext = EntryActivityContext.Lifecycle,
    ) {
        with(db) {
            entryActivityCommandDao().insertIdempotent(
                EntryActivity(entryId = entryId, activityType = activityType, context = context, createdAt = now).toEntity()
            )
        }
    }

}
