package com.aozijx.passly.data.local.database.dao.attachment

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aozijx.passly.data.local.database.entity.RevisionAttachmentRefEntity

@Dao
interface RevisionAttachmentRefDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllStrict(refs: List<RevisionAttachmentRefEntity>)

    @Query("SELECT * FROM revision_attachment_refs WHERE revisionId = :revisionId")
    suspend fun getByRevisionId(revisionId: String): List<RevisionAttachmentRefEntity>

    @Query("SELECT COUNT(*) FROM revision_attachment_refs WHERE resourceId = :resourceId")
    suspend fun countByResourceId(resourceId: String): Int
}
