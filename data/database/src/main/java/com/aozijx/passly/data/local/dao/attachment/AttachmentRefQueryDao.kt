package com.aozijx.passly.data.local.dao.attachment

import androidx.room.Dao
import androidx.room.Query
import com.aozijx.passly.data.model.entity.AttachmentRefEntity

@Dao
interface AttachmentRefQueryDao {
    @Query("SELECT * FROM attachment_refs WHERE attachmentId = :attachmentId LIMIT 1")
    suspend fun getById(attachmentId: String): AttachmentRefEntity?

    @Query("SELECT * FROM attachment_refs WHERE entryId = :entryId AND status = 'COMMITTED' ORDER BY displayOrder, createdAt")
    suspend fun getCommittedByEntryId(entryId: String): List<AttachmentRefEntity>

    @Query("SELECT * FROM attachment_refs WHERE entryId IN (:entryIds) AND status = 'COMMITTED' ORDER BY entryId, displayOrder, createdAt")
    suspend fun getCommittedByEntryIds(entryIds: List<String>): List<AttachmentRefEntity>

    @Query("SELECT * FROM attachment_refs WHERE stagingOwnerId = :stagingOwnerId AND status = 'PENDING' ORDER BY displayOrder, createdAt")
    suspend fun getPendingByOwner(stagingOwnerId: String): List<AttachmentRefEntity>

    @Query("SELECT COUNT(*) FROM attachment_refs WHERE entryId = :entryId AND status = 'COMMITTED'")
    suspend fun countCommittedByEntryId(entryId: String): Int
}
