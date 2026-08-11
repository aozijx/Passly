package com.aozijx.passly.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aozijx.passly.data.model.entity.AttachmentResourceEntity
import com.aozijx.passly.data.model.entity.AttachmentRefEntity
import com.aozijx.passly.data.model.entity.EntryEntity
import com.aozijx.passly.data.model.entity.EntryRevisionEntity
import com.aozijx.passly.data.model.entity.RevisionAttachmentRefEntity
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.attachment.AttachmentStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AttachmentReferenceLifecycleDaoTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(AttachmentRefConstraints)
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun identicalContentIsShared_andResourceIsRestrictedWhileReferenced() = runBlocking {
        insertEntry("entry-a")
        insertEntry("entry-b")
        database.attachmentResourceDao().insertStrict(resource())
        database.attachmentRefCommandDao().insertStrict(currentRef("attachment-a", "entry-a"))
        database.attachmentRefCommandDao().insertStrict(currentRef("attachment-b", "entry-b"))

        assertTrue(
            runCatching {
                database.openHelper.writableDatabase.execSQL(
                    "DELETE FROM attachment_resources WHERE resourceId = ?",
                    arrayOf(RESOURCE_ID),
                )
            }.isFailure
        )
        database.attachmentRefCommandDao().deleteById("attachment-a")
        assertTrue(database.attachmentResourceDao().getUnreferenced().isEmpty())
        database.attachmentRefCommandDao().deleteById("attachment-b")
        assertEquals(listOf(RESOURCE_ID), database.attachmentResourceDao().getUnreferenced().map { it.resourceId })
    }

    @Test
    fun revisionReferenceRetainsContentUntilRevisionIsDeleted() = runBlocking {
        insertEntry("entry-a")
        database.attachmentResourceDao().insertStrict(resource())
        database.attachmentRefCommandDao().insertStrict(currentRef("attachment-a", "entry-a"))
        database.entryRevisionCommandDao().insertStrict(
            EntryRevisionEntity(
                revisionId = "revision-a",
                entryId = "entry-a",
                version = 1,
                entryContentCipher = byteArrayOf(1),
                sensitiveFieldCipherSet = byteArrayOf(1),
                changeType = "value_changed",
                createdAt = 1L,
            )
        )
        database.revisionAttachmentRefDao().insertAllStrict(
            listOf(
                RevisionAttachmentRefEntity(
                    revisionId = "revision-a",
                    attachmentId = "attachment-a",
                    resourceId = RESOURCE_ID,
                    fileName = "shared.bin",
                    mimeType = "application/octet-stream",
                    displayOrder = 0,
                    createdAt = 1L,
                )
            )
        )

        database.attachmentRefCommandDao().deleteById("attachment-a")
        assertTrue(database.attachmentResourceDao().getUnreferenced().isEmpty())
        database.entryRevisionCommandDao().deleteByEntryId("entry-a")
        assertEquals(0, database.revisionAttachmentRefDao().countByResourceId(RESOURCE_ID))
        assertEquals(listOf(RESOURCE_ID), database.attachmentResourceDao().getUnreferenced().map { it.resourceId })
    }

    @Test
    fun databaseRejectsInvalidPendingAndCommittedOwnership() = runBlocking {
        insertEntry("entry-a")
        database.attachmentResourceDao().insertStrict(resource())
        val invalidPending = currentRef("pending", "entry-a").copy(
            status = AttachmentStatus.PENDING.name,
            stagingOwnerId = "editor-session",
        )
        assertTrue(runCatching {
            database.attachmentRefCommandDao().insertStrict(invalidPending)
        }.isFailure)
        val invalidCommitted = currentRef("committed", "entry-a").copy(
            stagingOwnerId = "editor-session",
        )
        assertTrue(runCatching {
            database.attachmentRefCommandDao().insertStrict(invalidCommitted)
        }.isFailure)
        database.attachmentRefCommandDao().insertStrict(
            AttachmentRefEntity(
                attachmentId = "pending-valid",
                resourceId = RESOURCE_ID,
                entryId = null,
                stagingOwnerId = "editor-session",
                fileName = "readme.md",
                mimeType = null,
                status = AttachmentStatus.PENDING.name,
            )
        )
    }

    private suspend fun insertEntry(id: String) {
        database.entryCommandDao().insertStrict(
            EntryEntity(
                entryId = id,
                entryType = EntryType.LOGIN,
                summaryBlob = byteArrayOf(),
                createdAt = 1L,
                updatedAt = 1L,
            )
        )
    }

    private fun resource() = AttachmentResourceEntity(
        resourceId = RESOURCE_ID,
        fileSize = 4L,
        createdAt = 1L,
    )

    private fun currentRef(id: String, entryId: String) = AttachmentRefEntity(
        attachmentId = id,
        entryId = entryId,
        resourceId = RESOURCE_ID,
        fileName = "shared.bin",
        mimeType = "application/octet-stream",
        status = AttachmentStatus.COMMITTED.name,
        stagingOwnerId = null,
        createdAt = 1L,
    )

    private companion object {
        const val RESOURCE_ID = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    }
}
