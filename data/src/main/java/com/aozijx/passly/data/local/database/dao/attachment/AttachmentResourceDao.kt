package com.aozijx.passly.data.local.database.dao.attachment

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aozijx.passly.data.local.database.entity.AttachmentResourceEntity
import com.aozijx.passly.data.local.database.entity.AttachmentResourceState

@Dao
interface AttachmentResourceDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStrict(resource: AttachmentResourceEntity)

    @Query("SELECT * FROM attachment_resources WHERE resourceId = :resourceId LIMIT 1")
    suspend fun getById(resourceId: String): AttachmentResourceEntity?

    @Query(
        """
        SELECT * FROM attachment_resources AS resources
        WHERE NOT EXISTS (
            SELECT 1 FROM attachment_refs AS current_refs
            WHERE current_refs.resourceId = resources.resourceId
        ) AND NOT EXISTS (
            SELECT 1 FROM revision_attachment_refs AS history_refs
            WHERE history_refs.resourceId = resources.resourceId
        )
        """
    )
    suspend fun getUnreferenced(): List<AttachmentResourceEntity>

    @Query("UPDATE attachment_resources SET lifecycleState = :state WHERE resourceId IN (:resourceIds)")
    suspend fun updateState(resourceIds: List<String>, state: String): Int

    @Query("UPDATE attachment_resources SET lifecycleState = :state WHERE resourceId = :resourceId")
    suspend fun updateState(resourceId: String, state: String): Int

    @Query(
        """
        UPDATE attachment_resources
        SET lifecycleState = :state, enqueuedAt = :now, attemptCount = 0, lastAttemptAt = NULL
        WHERE resourceId IN (:resourceIds)
        """
    )
    suspend fun markPendingGc(resourceIds: List<String>, state: String, now: Long): Int

    @Query(
        """
        UPDATE attachment_resources
        SET lifecycleState = :state, enqueuedAt = NULL, attemptCount = 0, lastAttemptAt = NULL
        WHERE resourceId = :resourceId
        """
    )
    suspend fun reactivate(resourceId: String, state: String): Int

    @Query(
        """
        SELECT * FROM attachment_resources
        WHERE lifecycleState = :state
        ORDER BY enqueuedAt
        LIMIT :limit
        """
    )
    suspend fun getPendingGc(limit: Int, state: String = AttachmentResourceState.PENDING_GC): List<AttachmentResourceEntity>

    @Query(
        """
        UPDATE attachment_resources
        SET attemptCount = attemptCount + 1, lastAttemptAt = :now
        WHERE resourceId = :resourceId
        """
    )
    suspend fun recordAttempt(resourceId: String, now: Long): Int

    @Query("SELECT COUNT(*) FROM attachment_refs WHERE resourceId = :resourceId")
    suspend fun currentRefCount(resourceId: String): Int

    @Query("SELECT COUNT(*) FROM revision_attachment_refs WHERE resourceId = :resourceId")
    suspend fun revisionRefCount(resourceId: String): Int

    @Query("DELETE FROM attachment_resources WHERE resourceId = :resourceId AND lifecycleState = :pendingState")
    suspend fun deletePending(resourceId: String, pendingState: String = AttachmentResourceState.PENDING_GC): Int

}
