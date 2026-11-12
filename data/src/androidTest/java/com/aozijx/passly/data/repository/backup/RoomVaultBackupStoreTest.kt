package com.aozijx.passly.data.repository.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aozijx.passly.core.crypto.AesGcmCryptoEngine
import com.aozijx.passly.core.crypto.AttachmentContentProtector
import com.aozijx.passly.core.crypto.FieldEncryptor
import com.aozijx.passly.core.observability.ErrorCode
import com.aozijx.passly.core.observability.EventKey
import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.TraceResumeResult
import com.aozijx.passly.core.observability.TraceScope
import com.aozijx.passly.core.observability.trace.TraceToken
import com.aozijx.passly.core.platform.VaultResourcePaths
import com.aozijx.passly.data.codec.entry.SecretBundleCodec
import com.aozijx.passly.data.codec.entry.SecretFieldCodec
import com.aozijx.passly.data.local.database.DatabaseClock
import com.aozijx.passly.data.local.database.DatabaseProvider
import com.aozijx.passly.data.local.database.DatabaseSchema
import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.local.database.session.AppDatabaseSessionResource
import com.aozijx.passly.data.local.database.session.DatabaseSessionEventLogger
import com.aozijx.passly.data.repository.attachment.AttachmentResourceGarbageCollector
import com.aozijx.passly.data.repository.entry.SecretFieldStore
import com.aozijx.passly.data.testing.IsolatedTestContext
import com.aozijx.passly.data.testing.TestFieldKeyProvider
import com.aozijx.passly.data.testing.TestSensitiveFieldCryptor
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.backup.model.VaultBackupReadOptions
import com.aozijx.passly.domain.backup.model.VaultBackupResource
import com.aozijx.passly.domain.backup.model.VaultBackupResourceKind
import com.aozijx.passly.domain.backup.model.VaultBackupRestoreMode
import com.aozijx.passly.domain.backup.model.VaultBackupSnapshot
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryIcon
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.runtime.session.SessionKeySource
import java.io.File
import java.security.MessageDigest
import javax.inject.Provider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomVaultBackupStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var session: AppDatabaseSession
    private lateinit var access: SecureSessionAccessState
    private lateinit var fieldKeyProvider: TestFieldKeyProvider
    private lateinit var store: RoomVaultBackupStore

    @Before
    fun setUp() {
        System.loadLibrary("sqlcipher")
        context = IsolatedTestContext(ApplicationProvider.getApplicationContext(), temporaryFolder.root)
        context.deleteDatabase(DatabaseSchema.DATABASE_NAME)
        val logger = NoOpLogger
        access = TestSessionAccess()
        session =
            AppDatabaseSession(
                resource = AppDatabaseSessionResource(DatabaseProvider(context, logger)),
                keySource = SessionKeySource { DATABASE_KEY.copyOf() },
                eventLogger = DatabaseSessionEventLogger(logger),
                accessState = Provider { access },
            )
        assertEquals(null, runBlocking { session.unlock() })
        fieldKeyProvider = TestFieldKeyProvider(DATABASE_KEY)
        val secretStore =
            SecretFieldStore(
                SecretFieldCodec(TestSensitiveFieldCryptor),
                SecretBundleCodec(FieldEncryptor(fieldKeyProvider, AesGcmCryptoEngine())),
                DatabaseClock(),
            )
        val attachmentGc = AttachmentResourceGarbageCollector(context, session, access, logger)
        store =
            RoomVaultBackupStore(
                context = context,
                databaseSession = session,
                secretFieldStore = secretStore,
                attachmentContentProtector = TestAttachmentContentProtector,
                attachmentGarbageCollector = attachmentGc,
                logger = logger,
            )
    }

    @After
    fun tearDown() {
        runBlocking { session.seal() }
        fieldKeyProvider.clear()
        VaultResourcePaths.resourceDirectories(context).forEach { it.deleteRecursively() }
        context.deleteDatabase(DatabaseSchema.DATABASE_NAME)
    }

    @Test
    fun readHonorsDeletedAndTypeFilters() =
        runBlocking {
            insertAccount("active", "Active")
            insertAccount("deleted", "Deleted", deletedAt = 3L)
            insertNote("note", "Not an account")

            val snapshot =
                store.read(
                    VaultBackupReadOptions(
                        includeDeleted = false,
                        includedEntryTypes = setOf(EntryType.ACCOUNT),
                        includeIcons = false,
                        includeAttachments = false,
                        maxResourceBytes = 1024L,
                        maxTotalResourceBytes = 4096L,
                    ),
                )

            assertEquals(listOf(EntryId("active")), snapshot.entries.map(Entry::id))
        }

    @Test
    fun appendSkipsExistingEntryWithoutReplacingIt() =
        runBlocking {
            insertAccount("existing", "Keep")

            val result =
                store.restore(
                    snapshot(entries = listOf(account("existing", "Replace"))),
                    VaultBackupRestoreMode.APPEND,
                    onCommitted = {},
                )

            assertEquals(0, result.importedEntryCount)
            assertEquals(1, result.existingEntryCount)
            assertEquals("Keep", session.queryForMaintenance { entryQueryDao().getById("existing")?.title })
        }

    @Test
    fun overwriteReplacesEntriesWithinTheDatabaseSession() =
        runBlocking {
            insertAccount("old", "Old")
            var commitNotifications = 0

            val result =
                store.restore(
                    snapshot(entries = listOf(account("new", "New"))),
                    VaultBackupRestoreMode.OVERWRITE,
                    onCommitted = {
                        commitNotifications++
                        assertFalse(session.queryForMaintenance { entryQueryDao().exists("old") })
                        assertEquals("New", session.queryForMaintenance { entryQueryDao().getById("new")?.title })
                    },
                )

            assertEquals(1, commitNotifications)
            assertEquals(1, result.importedEntryCount)
            assertEquals(0, result.existingEntryCount)
            assertFalse(session.queryForMaintenance { entryQueryDao().exists("old") })
            assertEquals("New", session.queryForMaintenance { entryQueryDao().getById("new")?.title })
        }

    @Test
    fun restoreAndReadRoundTripAttachmentBytesAndMetadata() =
        runBlocking {
            val content = "attachment payload".toByteArray()
            val source =
                VaultBackupResource(
                    id = "attachment-1",
                    entryId = EntryId("entry-1"),
                    kind = VaultBackupResourceKind.ATTACHMENT,
                    fileName = "notes.txt",
                    mimeType = "text/plain",
                    createdAt = 5L,
                    content = content,
                )

            store.restore(
                snapshot(entries = listOf(account("entry-1", "Entry")), resources = listOf(source)),
                VaultBackupRestoreMode.OVERWRITE,
                onCommitted = {},
            )

            val restored =
                store
                    .read(
                        VaultBackupReadOptions(
                            includeDeleted = true,
                            includedEntryTypes = setOf(EntryType.ACCOUNT),
                            includeIcons = false,
                            includeAttachments = true,
                            maxResourceBytes = 1024L,
                            maxTotalResourceBytes = 4096L,
                        ),
                    ).resources
                    .single()
            assertEquals(source.id, restored.id)
            assertEquals(source.entryId, restored.entryId)
            assertEquals(source.kind, restored.kind)
            assertEquals(source.fileName, restored.fileName)
            assertEquals(source.mimeType, restored.mimeType)
            assertEquals(source.createdAt, restored.createdAt)
            assertArrayEquals(content, restored.content)
        }

    @Test
    fun restoreAndReadRoundTripIconAndRewriteLocalPath() =
        runBlocking {
            val content = byteArrayOf(0x52, 0x49, 0x46, 0x46, 1)
            val entry =
                account("entry-icon", "Icon").copy(
                    profile =
                        account("entry-icon", "Icon").profile.copy(
                            icon = EntryIcon(customReference = "old/private/path.webp"),
                        ),
                )
            val icon =
                VaultBackupResource(
                    id = entry.id.value,
                    entryId = entry.id,
                    kind = VaultBackupResourceKind.ICON,
                    fileName = "private-icon.webp",
                    mimeType = "image/webp",
                    createdAt = null,
                    content = content,
                )

            store.restore(
                snapshot(entries = listOf(entry), resources = listOf(icon)),
                VaultBackupRestoreMode.OVERWRITE,
                onCommitted = {},
            )

            val restoredPath = requireNotNull(session.queryForMaintenance { entryQueryDao().getById(entry.id.value)?.iconCustomReference })
            assertTrue(File(restoredPath).isFile)
            assertTrue(restoredPath.contains("favicon_restored_"))
            val restored =
                store
                    .read(
                        VaultBackupReadOptions(
                            includeDeleted = true,
                            includedEntryTypes = setOf(EntryType.ACCOUNT),
                            includeIcons = true,
                            includeAttachments = false,
                            maxResourceBytes = 1024L,
                            maxTotalResourceBytes = 4096L,
                        ),
                    ).resources
                    .single()
            assertEquals(VaultBackupResourceKind.ICON, restored.kind)
            assertArrayEquals(content, restored.content)
        }

    @Test
    fun failedRestoreRollsBackDatabaseAndAlreadyStagedIconFile() =
        runBlocking {
            insertAccount("existing", "Keep")
            val entry = account("entry-rollback", "Rollback")
            val icon =
                VaultBackupResource(
                    id = "domain-icon-rollback",
                    entryId = entry.id,
                    kind = VaultBackupResourceKind.ICON,
                    fileName = "icon.webp",
                    mimeType = "image/webp",
                    createdAt = null,
                    content = byteArrayOf(0x52, 0x49, 0x46, 0x46),
                )
            val attachment =
                VaultBackupResource(
                    id = "attachment-rollback",
                    entryId = entry.id,
                    kind = VaultBackupResourceKind.ATTACHMENT,
                    fileName = "fail.txt",
                    mimeType = "text/plain",
                    createdAt = 6L,
                    content = "will fail encryption".toByteArray(),
                )
            TestAttachmentContentProtector.failEncrypt = true

            val failure =
                try {
                    store.restore(
                        snapshot(listOf(entry), listOf(icon, attachment)),
                        VaultBackupRestoreMode.OVERWRITE,
                        onCommitted = {},
                    )
                    null
                } catch (error: IllegalStateException) {
                    error
                } finally {
                    TestAttachmentContentProtector.failEncrypt = false
                }

            assertEquals("Injected attachment encryption failure", failure?.message)
            assertTrue(session.queryForMaintenance { entryQueryDao().exists("existing") })
            assertFalse(session.queryForMaintenance { entryQueryDao().exists("entry-rollback") })
            assertTrue(
                VaultResourcePaths
                    .vaultImagesDir(context)
                    .listFiles()
                    .orEmpty()
                    .none { it.name.startsWith("favicon_restored_") },
            )
        }

    private suspend fun insertAccount(
        id: String,
        title: String,
        deletedAt: Long? = null,
    ) = session.transactionForMaintenance {
        entryCommandDao().insertStrict(
            EntryEntity(
                entryId = id,
                entryType = EntryType.ACCOUNT,
                title = title,
                createdAt = 1L,
                updatedAt = 2L,
                deletedAt = deletedAt,
            ),
        )
    }

    private suspend fun insertNote(
        id: String,
        title: String,
    ) = session.transactionForMaintenance {
        entryCommandDao().insertStrict(
            EntryEntity(
                entryId = id,
                entryType = EntryType.NOTE,
                title = title,
                createdAt = 1L,
                updatedAt = 2L,
            ),
        )
    }

    private fun snapshot(
        entries: List<Entry>,
        resources: List<VaultBackupResource> = emptyList(),
    ) = VaultBackupSnapshot(entries, emptyList(), resources)

    private fun account(
        id: String,
        title: String,
    ) = Entry(
        identity =
            EntryIdentity(
                id = EntryId(id),
                type = EntryType.ACCOUNT,
                version = EntryVersion.INITIAL,
                timestamps = EntryTimestamps(createdAtMs = 1L, updatedAtMs = 2L),
            ),
        profile = EntryProfile(title),
    )

    private class TestSessionAccess : SecureSessionAccessState {
        override val authenticationState = MutableStateFlow<AuthenticationState>(AuthenticationState.Authenticated(1L))

        override fun isUnlocked() = true
    }

    private object TestAttachmentContentProtector : AttachmentContentProtector {
        var failEncrypt = false

        override suspend fun contentId(content: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(content).joinToString("") { "%02x".format(it) }

        override suspend fun encrypt(
            content: ByteArray,
            resourceId: String,
        ): ByteArray {
            check(!failEncrypt) { "Injected attachment encryption failure" }
            return content.copyOf()
        }

        override suspend fun decrypt(
            encrypted: ByteArray,
            resourceId: String,
        ) = encrypted.copyOf()

        override suspend fun verifyContentId(
            content: ByteArray,
            resourceId: String,
        ) = contentId(content) == resourceId
    }

    private inline fun <reified T> unusedPort(): T =
        java.lang.reflect.Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
        ) { _, _, _ -> error("Unexpected test port call") } as T

    private object NoOpLogger : Logger {
        override fun debug(
            event: EventKey,
            fields: LogFields,
        ) = Unit

        override fun info(
            event: EventKey,
            fields: LogFields,
        ) = Unit

        override fun degraded(
            event: EventKey,
            errorCode: ErrorCode,
            reason: ReasonCode,
            fields: LogFields,
        ) = Unit

        override fun warn(
            event: EventKey,
            errorCode: ErrorCode,
            reason: ReasonCode,
            throwable: Throwable,
            fields: LogFields,
        ) = Unit

        override fun error(
            event: EventKey,
            errorCode: ErrorCode,
            reason: ReasonCode,
            throwable: Throwable,
            fields: LogFields,
        ) = Unit

        override fun fatal(
            event: EventKey,
            errorCode: ErrorCode,
            reason: ReasonCode,
            throwable: Throwable,
            fields: LogFields,
        ) = Unit

        override suspend fun <T> withRootTrace(
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ) = TestTraceScope.block()

        override suspend fun <T> inSpan(
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ) = TestTraceScope.block()

        override fun captureTraceToken(singleUse: Boolean): TraceToken = error("Unused")

        override suspend fun <T> resumeTrace(
            token: TraceToken,
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ): TraceResumeResult<T> = error("Unused")
    }

    private object TestTraceScope : TraceScope {
        override val traceId = TraceId("backup_store")
        override val spanId = SpanId("backup_store")
        override val parentSpanId: SpanId? = null
    }

    private companion object {
        val DATABASE_KEY = "Vault backup store test key".toByteArray()
    }
}
