package com.aozijx.passly.data.local.database.dao.attachment

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aozijx.passly.data.local.database.entity.AttachmentGcQueueEntity

@Dao
interface AttachmentGcQueueDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(items: List<AttachmentGcQueueEntity>)

    @Query("SELECT * FROM attachment_gc_queue ORDER BY enqueuedAt LIMIT :limit")
    suspend fun getPending(limit: Int): List<AttachmentGcQueueEntity>

    @Query("DELETE FROM attachment_gc_queue WHERE resourceId = :resourceId")
    suspend fun delete(resourceId: String): Int

    @Query("UPDATE attachment_gc_queue SET attemptCount = attemptCount + 1, lastAttemptAt = :now WHERE resourceId = :resourceId")
    suspend fun recordAttempt(resourceId: String, now: Long): Int
}
