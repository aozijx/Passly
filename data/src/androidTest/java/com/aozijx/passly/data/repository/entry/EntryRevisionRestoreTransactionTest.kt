package com.aozijx.passly.data.repository.entry

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aozijx.passly.core.crypto.AesGcmCryptoEngine
import com.aozijx.passly.core.crypto.FieldEncryptor
import com.aozijx.passly.data.codec.entry.SecretBundleCodec
import com.aozijx.passly.data.codec.revision.EntryContentSnapshotCodec
import com.aozijx.passly.data.codec.revision.SensitiveRevisionSnapshotCodec
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.data.local.database.entity.EntryRevisionEntity
import com.aozijx.passly.data.local.database.entity.EntrySecretFieldEntity
import com.aozijx.passly.data.repository.entry.command.EntryActivityWriter
import com.aozijx.passly.data.repository.entry.command.EntryRevisionWriter
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.model.SensitiveRevisionAccessAction
import com.aozijx.passly.domain.access.port.AuthorizationPermitVerifier
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.history.EntryRevisionId
import com.aozijx.passly.domain.entry.model.history.RevisionChange
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.security.dek.FieldKeyManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EntryRevisionRestoreTransactionTest {
    private lateinit var database: AppDatabase
    private lateinit var fieldKeyManager: FieldKeyManager
    private lateinit var contentCodec: EntryContentSnapshotCodec
    private lateinit var bundleCodec: SecretBundleCodec
    private val sensitiveCodec = SensitiveRevisionSnapshotCodec()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        fieldKeyManager = FieldKeyManager().apply {
            deriveAndSet(ByteArray(32) { (it + 11).toByte() })
        }
        val fieldEncryptor = FieldEncryptor(fieldKeyManager, AesGcmCryptoEngine())
        contentCodec = EntryContentSnapshotCodec(fieldEncryptor)
        bundleCodec = SecretBundleCodec(fieldEncryptor)
    }

    @After
    fun tearDown() {
        database.close()
        fieldKeyManager.clear()
    }

    @Test
    fun restore_replacesSnapshotAndCreatesReversibleLatestRevision() = runBlocking {
        seedCurrentAndHistoricalRevision()
        val verifier = ExactScopeVerifier(
            AuthorizationScope.SensitiveRevision(
                entryId = EntryId(ENTRY_ID),
                revisionId = REVISION_ID,
                fieldKeys = setOf(SensitiveFieldKey.PASSWORD),
                action = SensitiveRevisionAccessAction.RESTORE,
            ),
        )
        val transaction = EntryRevisionRestoreTransaction(
            contentCodec = contentCodec,
            bundleCodec = bundleCodec,
            sensitiveCodec = sensitiveCodec,
            permitConsumer = RevisionPermitConsumer(verifier),
            revisionWriter = EntryRevisionWriter(contentCodec, sensitiveCodec),
            activityWriter = EntryActivityWriter(),
        )

        val restoredVersion = database.withTransaction {
            transaction.restore(
                db = database,
                entryId = EntryId(ENTRY_ID),
                revisionId = EntryRevisionId(REVISION_ID),
                expectedVersion = EntryVersion(2),
                permit = TestPermit,
                now = 100L,
            )
        }

        val restored = database.entryQueryDao().getById(ENTRY_ID)!!
        val restoredRows = database.secretFieldQueryDao().getAll(ENTRY_ID)
        val restoredBundle = restoredRows.single { it.fieldKey == SecretBundleCodec.FIELD_KEY }
        val restoredPassword = restoredRows.single { it.fieldKey == SensitiveFieldKey.PASSWORD.name }
        val latest = database.entryRevisionQueryDao().getLatest(ENTRY_ID)!!

        assertEquals(3, restoredVersion.value)
        assertEquals(3, restored.version)
        assertEquals("Historical", restored.title)
        assertEquals("historical notes", bundleCodec.decrypt(restoredBundle.valueCipher, ENTRY_ID).notes)
        assertArrayEquals(HISTORICAL_PASSWORD_CIPHER, restoredPassword.valueCipher)
        assertEquals(3, latest.version)
        assertEquals(RevisionChange.VERSION_RESTORED.name, latest.changeType)
        assertEquals(3, database.entryRevisionQueryDao().getByEntryId(ENTRY_ID).size)
        assertEquals(
            "Historical",
            contentCodec.decrypt(latest.entryContentCipher, ENTRY_ID).summary.title,
        )
        assertEquals(
            1,
            database.entryActivityQueryDao().countByEntryIdAndType(ENTRY_ID, ActivityType.RESTORE),
        )
        assertTrue(verifier.consumed)
    }

    @Test
    fun restore_withoutSensitiveFields_doesNotRequireOrConsumePermit() = runBlocking {
        seedCurrentAndHistoricalRevision(includePassword = false)
        val verifier = ExactScopeVerifier(null)
        val transaction = EntryRevisionRestoreTransaction(
            contentCodec = contentCodec,
            bundleCodec = bundleCodec,
            sensitiveCodec = sensitiveCodec,
            permitConsumer = RevisionPermitConsumer(verifier),
            revisionWriter = EntryRevisionWriter(contentCodec, sensitiveCodec),
            activityWriter = EntryActivityWriter(),
        )

        val restoredVersion = database.withTransaction {
            transaction.restore(
                db = database,
                entryId = EntryId(ENTRY_ID),
                revisionId = EntryRevisionId(REVISION_ID),
                expectedVersion = EntryVersion(2),
                permit = null,
                now = 100L,
            )
        }

        assertEquals(3, restoredVersion.value)
        assertEquals("Historical", database.entryQueryDao().getById(ENTRY_ID)?.title)
        assertTrue(!verifier.consumed)
    }

    @Test
    fun staleVersion_rollsBackWithoutChangingCurrentEntry() = runBlocking {
        seedCurrentAndHistoricalRevision()
        val transaction = EntryRevisionRestoreTransaction(
            contentCodec = contentCodec,
            bundleCodec = bundleCodec,
            sensitiveCodec = sensitiveCodec,
            permitConsumer = RevisionPermitConsumer(ExactScopeVerifier(null)),
            revisionWriter = EntryRevisionWriter(contentCodec, sensitiveCodec),
            activityWriter = EntryActivityWriter(),
        )

        val failure = runCatching {
            database.withTransaction {
                transaction.restore(
                    db = database,
                    entryId = EntryId(ENTRY_ID),
                    revisionId = EntryRevisionId(REVISION_ID),
                    expectedVersion = EntryVersion(1),
                    permit = null,
                    now = 100L,
                )
            }
        }.exceptionOrNull()

        assertTrue(failure != null)
        assertEquals("Current", database.entryQueryDao().getById(ENTRY_ID)?.title)
        assertEquals(2, database.entryRevisionQueryDao().getByEntryId(ENTRY_ID).size)
        assertEquals(0, database.entryActivityQueryDao().countByEntryId(ENTRY_ID))
    }

    private suspend fun seedCurrentAndHistoricalRevision(includePassword: Boolean = true) {
        database.entryCommandDao().insertStrict(
            EntryEntity(
                entryId = ENTRY_ID,
                entryType = EntryType.LOGIN,
                version = 2,
                title = "Current",
                createdAt = 1L,
                updatedAt = 2L,
            ),
        )
        val currentRows = buildList {
            add(
                EntrySecretFieldEntity(
                    entryId = ENTRY_ID,
                    fieldKey = SecretBundleCodec.FIELD_KEY,
                    valueCipher = bundleCodec.encrypt(
                        EntrySecret(
                            credential = LoginCredential(),
                            notes = "current notes",
                        ),
                        ENTRY_ID,
                    ),
                    keyVersion = 1,
                    updatedAt = 2L,
                ),
            )
            if (includePassword) add(
                EntrySecretFieldEntity(
                    entryId = ENTRY_ID,
                    fieldKey = SensitiveFieldKey.PASSWORD.name,
                    valueCipher = CURRENT_PASSWORD_CIPHER,
                    keyVersion = 1,
                    updatedAt = 2L,
                ),
            )
        }
        database.secretFieldCommandDao().insertAllStrict(currentRows)
        val historicalContent = contentCodec.encrypt(
            summary = EntryProfile(title = "Historical"),
            bundleSecret = EntrySecret(
                credential = LoginCredential(),
                notes = "historical notes",
            ),
            entryId = ENTRY_ID,
            links = emptyList(),
        )
        val historicalSensitive = sensitiveCodec.encode(
            if (includePassword) listOf(
                EntrySecretFieldEntity(
                    entryId = ENTRY_ID,
                    fieldKey = SensitiveFieldKey.PASSWORD.name,
                    valueCipher = HISTORICAL_PASSWORD_CIPHER,
                    keyVersion = 1,
                    updatedAt = 1L,
                ),
            ) else emptyList(),
        )
        database.entryRevisionCommandDao().insertAllStrict(
            listOf(
                EntryRevisionEntity(
                    revisionId = REVISION_ID,
                    entryId = ENTRY_ID,
                    version = 1,
                    entryContentCipher = historicalContent,
                    sensitiveFieldCipherSet = historicalSensitive,
                    changeType = RevisionChange.VALUE_CHANGED.name,
                    createdAt = 1L,
                ),
                EntryRevisionEntity(
                    revisionId = "revision-current",
                    entryId = ENTRY_ID,
                    version = 2,
                    entryContentCipher = contentCodec.encrypt(
                        summary = EntryProfile(title = "Current"),
                        bundleSecret = EntrySecret(
                            credential = LoginCredential(),
                            notes = "current notes",
                        ),
                        entryId = ENTRY_ID,
                        links = emptyList(),
                    ),
                    sensitiveFieldCipherSet = sensitiveCodec.encode(
                        database.secretFieldQueryDao().getAll(ENTRY_ID)
                            .filter { it.fieldKey != SecretBundleCodec.FIELD_KEY },
                    ),
                    changeType = RevisionChange.VALUE_CHANGED.name,
                    createdAt = 2L,
                ),
            ),
        )
    }

    private object TestPermit : AuthorizationPermit

    private class ExactScopeVerifier(
        private val acceptedScope: AuthorizationScope?,
    ) : AuthorizationPermitVerifier {
        var consumed: Boolean = false

        override fun consume(
            permit: AuthorizationPermit,
            expectedScope: AuthorizationScope,
        ): Boolean {
            consumed = true
            return permit === TestPermit && expectedScope == acceptedScope
        }
    }

    private companion object {
        const val ENTRY_ID = "entry-1"
        const val REVISION_ID = "revision-historical"
        val CURRENT_PASSWORD_CIPHER = byteArrayOf(1, 2, 3)
        val HISTORICAL_PASSWORD_CIPHER = byteArrayOf(9, 8, 7)
    }
}
