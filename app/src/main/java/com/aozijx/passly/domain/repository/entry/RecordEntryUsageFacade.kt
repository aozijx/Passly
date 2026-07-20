package com.aozijx.passly.domain.repository.entry

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.model.activity.ActivityType

/**
 * 领域门面：记录条目使用事件。
 * data 层实现通过 @Transaction 保证跨表原子性。
 */
interface RecordEntryUsageFacade {
    suspend fun record(entryId: String, type: ActivityType): AppResult<Unit>
}
