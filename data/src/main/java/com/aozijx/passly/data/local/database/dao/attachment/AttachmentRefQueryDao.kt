package com.aozijx.passly.data.local.database.dao.attachment

import androidx.room.Dao
import androidx.room.Query
import com.aozijx.passly.data.local.database.entity.AttachmentRefEntity

@Dao
interface AttachmentRefQueryDao {
    @Query("SELECT * FROM attachment_refs WHERE attachmentId = :attachmentId LIMIT 1")
    suspend fun getById(attachmentId: String): AttachmentRefEntity?

    @Query("SELECT * FROM attachment_refs WHERE entryId = :entryId ORDER BY displayOrder, createdAt")
    suspend fun getByEntryId(entryId: String): List<AttachmentRefEntity>

    @Query("SELECT * FROM attachment_refs WHERE entryId IN (:entryIds) ORDER BY entryId, displayOrder, createdAt")
    suspend fun getByEntryIds(entryIds: List<String>): List<AttachmentRefEntity>

    @Query("SELECT COUNT(*) FROM attachment_refs WHERE entryId = :entryId")
    suspend fun countByEntryId(entryId: String): Int
}
