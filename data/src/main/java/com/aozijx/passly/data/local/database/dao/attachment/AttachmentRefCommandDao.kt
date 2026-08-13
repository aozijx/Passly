package com.aozijx.passly.data.local.database.dao.attachment

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aozijx.passly.data.local.database.entity.AttachmentRefEntity

@Dao
interface AttachmentRefCommandDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStrict(attachment: AttachmentRefEntity)

    @Query("DELETE FROM attachment_refs WHERE attachmentId = :attachmentId")
    suspend fun deleteById(attachmentId: String): Int

    @Query("UPDATE attachment_refs SET status = 'COMMITTED', entryId = :entryId, stagingOwnerId = NULL WHERE stagingOwnerId = :stagingOwnerId AND status = 'PENDING'")
    suspend fun commitByOwner(stagingOwnerId: String, entryId: String): Int

    @Query("DELETE FROM attachment_refs WHERE stagingOwnerId = :stagingOwnerId AND status = 'PENDING'")
    suspend fun deletePendingByOwner(stagingOwnerId: String): Int
}
