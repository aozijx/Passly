package com.aozijx.passly.data.repository.entry

import android.content.Context
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aozijx.passly.core.crypto.AesGcmCryptoEngine
import com.aozijx.passly.core.crypto.FieldEncryptor
import com.aozijx.passly.data.codec.entry.SecretBundleCodec
import com.aozijx.passly.data.codec.revision.EntryContentSnapshotCodec
import com.aozijx.passly.data.codec.revision.SensitiveRevisionSnapshotCodec
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.local.database.TestDatabaseFactory
import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.data.local.database.entity.EntryRevisionEntity
import com.aozijx.passly.data.local.database.entity.EntrySecretFieldEntity
import com.aozijx.passly.data.repository.entry.command.EntryActivityWriter
import com.aozijx.passly.data.repository.entry.command.EntryRevisionHistoryRebuilder
import com.aozijx.passly.data.repository.entry.command.EntryRevisionWriter
import com.aozijx.passly.data.testing.TestFieldKeyProvider
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.history.RevisionChange
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.port.EntryIconResourceCleaner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EntryRevisionHistoryRebuilderTest {
    private lateinit var database: AppDatabase
    private lateinit var fieldKeyProvider: TestFieldKeyProvider
    private lateinit var contentCodec: EntryContentSnapshotCodec
    private lateinit var bundleCodec: SecretBundleCodec

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            TestDatabaseFactory
                .inMemoryDatabaseBuilder(context)
                .allowMainThreadQueries()
                .build()
        fieldKeyProvider = TestFieldKeyProvider(ByteArray(32) { (it + 17).toByte() })
        val fieldEncryptor = FieldEncryptor(fieldKeyProvider, AesGcmCryptoEngine())
        contentCodec = EntryContentSnapshotCodec(fieldEncryptor)
        bundleCodec = SecretBundleCodec(fieldEncryptor)
    }

    @After
    fun tearDown() {
        database.close()
        fieldKeyProvider.clear()
    }

    @Test
    fun rebuild_discardsObsoleteHistoryAndCreatesCurrentBaselinesForActiveAndDeletedEntries() =
        runBlocking {
            seedEntry(ENTRY_ONE, version = 8, title = "First", notes = "first note")
            seedEntry(
                ENTRY_TWO,
                version = 3,
                title = "Deleted",
                notes = "deleted note",
                deletedAt = 40L,
            )
            database.secretFieldCommandDao().insertAllStrict(
                listOf(
                    EntrySecretFieldEntity(
                        entryId = ENTRY_ONE,
                        fieldKey = SensitiveFieldKey.PASSWORD.name,
                        valueCipher = SENSITIVE_CIPHER,
                        keyVersion = 2,
                        updatedAt = 3L,
                    ),
                ),
            )
            seedObsoleteRevision(ENTRY_ONE, version = 7)
            seedObsoleteRevision(ENTRY_TWO, version = 2)
            val sensitiveCodec = SensitiveRevisionSnapshotCodec()
            val rebuilder =
                EntryRevisionHistoryRebuilder(
                    revisionWriter =
                        EntryRevisionWriter(
                            contentCodec,
                            sensitiveCodec,
                            EntryIconResourceCleaner { },
                            EntryActivityWriter(),
                        ),
                    bundleCodec = bundleCodec,
                )

            database.withTransaction {
                rebuilder.rebuildInTransaction(database, now = 100L)
            }

            assertEquals(8, database.entryQueryDao().getById(ENTRY_ONE)?.version)
            assertEquals(3, database.entryQueryDao().getById(ENTRY_TWO)?.version)
            assertBaseline(ENTRY_ONE, version = 8, title = "First", notes = "first note")
            assertBaseline(ENTRY_TWO, version = 3, title = "Deleted", notes = "deleted note")
            assertEquals(
                2,
                database
                    .entryRevisionQueryDao()
                    .observeByEntryId(ENTRY_ONE)
                    .first()
                    .size +
                    database
                        .entryRevisionQueryDao()
                        .observeByEntryId(ENTRY_TWO)
                        .first()
                        .size,
            )
            val activeSensitive =
                sensitiveCodec.decode(
                    database
                        .entryRevisionQueryDao()
                        .observeByEntryId(ENTRY_ONE)
                        .first()
                        .single()
                        .sensitiveFieldCipherSet,
                )
            assertEquals(listOf(SensitiveFieldKey.PASSWORD), activeSensitive.map { it.key })
            assertArrayEquals(SENSITIVE_CIPHER, activeSensitive.single().valueCipher)
            assertEquals(2, activeSensitive.single().keyVersion)
            val deletedSensitive =
                sensitiveCodec.decode(
                    database
                        .entryRevisionQueryDao()
                        .observeByEntryId(ENTRY_TWO)
                        .first()
                        .single()
                        .sensitiveFieldCipherSet,
                )
            assertTrue(deletedSensitive.isEmpty())
        }

    private suspend fun assertBaseline(
        entryId: String,
        version: Int,
        title: String,
        notes: String,
    ) {
        val revisions = database.entryRevisionQueryDao().observeByEntryId(entryId).first()
        assertEquals(1, revisions.size)
        val revision = revisions.single()
        assertEquals(version, revision.version)
        assertEquals(100L, revision.createdAt)
        val decoded = contentCodec.decrypt(revision.entryContentCipher, entryId, EntryType.LOGIN)
        assertEquals(title, decoded.summary.title)
        assertEquals(notes, decoded.secret.notes)
    }

    private suspend fun seedEntry(
        entryId: String,
        version: Int,
        title: String,
        notes: String,
        deletedAt: Long? = null,
    ) {
        database.entryCommandDao().insertStrict(
            EntryEntity(
                entryId = entryId,
                entryType = EntryType.LOGIN,
                version = version,
                title = title,
                createdAt = 1L,
                updatedAt = 2L,
                deletedAt = deletedAt,
            ),
        )
        database.secretFieldCommandDao().insertAllStrict(
            listOf(
                EntrySecretFieldEntity(
                    entryId = entryId,
                    fieldKey = SecretBundleCodec.FIELD_KEY,
                    valueCipher =
                        bundleCodec.encrypt(
                            EntrySecret(
                                credential = LoginCredential(),
                                notes = notes,
                            ),
                            entryId,
                            EntryType.LOGIN,
                        ),
                    keyVersion = 1,
                    updatedAt = 2L,
                ),
            ),
        )
    }

    private suspend fun seedObsoleteRevision(
        entryId: String,
        version: Int,
    ) {
        database.entryRevisionCommandDao().insertStrict(
            EntryRevisionEntity(
                revisionId = "revision-$entryId",
                entryId = entryId,
                version = version,
                entryContentCipher = byteArrayOf(1, 2, 3),
                sensitiveFieldCipherSet = byteArrayOf(),
                changeType = RevisionChange.VALUE_CHANGED.name,
                createdAt = 9L,
            ),
        )
    }

    private companion object {
        const val ENTRY_ONE = "entry-1"
        const val ENTRY_TWO = "entry-2"
        val SENSITIVE_CIPHER = byteArrayOf(4, 2, 7, 1)
    }
}
