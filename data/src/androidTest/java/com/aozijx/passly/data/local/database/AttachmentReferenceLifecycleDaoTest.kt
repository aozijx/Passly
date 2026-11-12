package com.aozijx.passly.data.local.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aozijx.passly.data.local.database.entity.AttachmentRefEntity
import com.aozijx.passly.data.local.database.entity.AttachmentResourceEntity
import com.aozijx.passly.data.local.database.entity.AttachmentResourceState
import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.data.local.database.entity.EntryRevisionEntity
import com.aozijx.passly.data.local.database.entity.RevisionAttachmentRefEntity
import com.aozijx.passly.domain.entry.model.EntryType
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
        database =
            TestDatabaseFactory
                .inMemoryDatabaseBuilder(context)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun identicalContentIsShared_andResourceIsRestrictedWhileReferenced() =
        runBlocking {
            insertEntry("entry-a")
            insertEntry("entry-b")
            database.attachmentResourceDao().insertStrict(resource())
            database.attachmentRefCommandDao().insertStrict(currentRef("attachment-a", "entry-a"))
            database.attachmentRefCommandDao().insertStrict(currentRef("attachment-b", "entry-b"))

            assertDatabaseFailureContaining(
                expectedMessage = "FOREIGN KEY constraint failed",
                failure =
                    runCatching {
                        database.openHelper.writableDatabase.execSQL(
                            "DELETE FROM attachment_resources WHERE resourceId = ?",
                            arrayOf(RESOURCE_ID),
                        )
                    }.exceptionOrNull(),
            )
            database.attachmentRefCommandDao().deleteById("attachment-a")
            assertTrue(database.attachmentResourceDao().getUnreferenced().isEmpty())
            database.attachmentRefCommandDao().deleteById("attachment-b")
            assertEquals(listOf(RESOURCE_ID), database.attachmentResourceDao().getUnreferenced().map { it.resourceId })
        }

    @Test
    fun revisionReferenceRetainsContentUntilRevisionIsDeleted() =
        runBlocking {
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
                ),
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
                    ),
                ),
            )

            database.attachmentRefCommandDao().deleteById("attachment-a")
            assertTrue(database.attachmentResourceDao().getUnreferenced().isEmpty())
            database.entryRevisionCommandDao().deleteByEntryId("entry-a")
            assertEquals(0, database.revisionAttachmentRefDao().countByResourceId(RESOURCE_ID))
            assertEquals(listOf(RESOURCE_ID), database.attachmentResourceDao().getUnreferenced().map { it.resourceId })
        }

    @Test
    fun databaseRequiresAnExistingEntryAndActiveResource() =
        runBlocking {
            insertEntry("entry-a")
            database.attachmentResourceDao().insertStrict(resource())
            assertDatabaseFailureContaining(
                expectedMessage = "FOREIGN KEY constraint failed",
                failure =
                    runCatching {
                        database.attachmentRefCommandDao().insertStrict(currentRef("missing-entry", "missing-entry"))
                    }.exceptionOrNull(),
            )

            val pendingResourceId = "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"
            database.attachmentResourceDao().insertStrict(
                resource(resourceId = pendingResourceId, lifecycleState = AttachmentResourceState.PENDING_GC),
            )
            assertDatabaseFailureContaining(
                expectedMessage = "invalid attachment ref state",
                failure =
                    runCatching {
                        database.attachmentRefCommandDao().insertStrict(
                            currentRef("inactive-resource", "entry-a").copy(resourceId = pendingResourceId),
                        )
                    }.exceptionOrNull(),
            )
        }

    private fun assertDatabaseFailureContaining(
        expectedMessage: String,
        failure: Throwable?,
    ) {
        assertTrue(
            "expected database failure containing '$expectedMessage', but was $failure",
            generateSequence(failure) { it.cause }.any { it.message?.contains(expectedMessage) == true },
        )
    }

    private suspend fun insertEntry(id: String) {
        database.entryCommandDao().insertStrict(
            EntryEntity(
                entryId = id,
                entryType = EntryType.LOGIN,
                title = "entry",
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )
    }

    private fun resource(
        resourceId: String = RESOURCE_ID,
        lifecycleState: String = AttachmentResourceState.ACTIVE,
    ) = AttachmentResourceEntity(
        resourceId = resourceId,
        fileSize = 4L,
        lifecycleState = lifecycleState,
        createdAt = 1L,
    )

    private fun currentRef(
        id: String,
        entryId: String,
    ) = AttachmentRefEntity(
        attachmentId = id,
        entryId = entryId,
        resourceId = RESOURCE_ID,
        fileName = "shared.bin",
        mimeType = "application/octet-stream",
        createdAt = 1L,
    )

    private companion object {
        const val RESOURCE_ID = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    }
}
