package com.aozijx.passly.data.local.database.recovery

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aozijx.passly.core.platform.VaultResourcePaths
import com.aozijx.passly.data.database.model.DatabaseRecoveryStatus
import com.aozijx.passly.data.local.database.DatabaseSchema
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DatabaseRecoveryStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = DatabaseRecoveryStore(context)

    @Before
    fun setUp() = clean()

    @After
    fun tearDown() = clean()

    @Test
    fun preserveListsVerifiesAndDeletesPackage() {
        val database = context.getDatabasePath(DatabaseSchema.DATABASE_NAME)
        database.parentFile?.mkdirs()
        database.writeBytes(byteArrayOf(1, 2, 3, 4))
        val attachment = File(
            context.filesDir,
            "${VaultResourcePaths.ATTACHMENTS}/content/resource.enc",
        )
        attachment.parentFile?.mkdirs()
        attachment.writeBytes(byteArrayOf(5, 6, 7))

        val id = requireNotNull(store.preserveAndClearActiveVault())

        assertFalse(database.exists())
        assertFalse(File(context.filesDir, VaultResourcePaths.ATTACHMENTS).exists())
        val listed = store.listPackages().single()
        assertEquals(id, listed.id)
        assertEquals(DatabaseRecoveryStatus.PENDING_SCAN, listed.status)
        assertEquals(7L, listed.sizeBytes)
        assertTrue(store.verify(id).databaseDirectory.isDirectory)

        store.updateStatus(id, DatabaseRecoveryStatus.RECOVERABLE)
        assertEquals(DatabaseRecoveryStatus.RECOVERABLE, store.listPackages().single().status)
        store.delete(id)
        assertTrue(store.listPackages().isEmpty())
    }

    @Test
    fun verificationRejectsContentTampering() {
        val database = context.getDatabasePath(DatabaseSchema.DATABASE_NAME)
        database.parentFile?.mkdirs()
        database.writeBytes(byteArrayOf(1, 2, 3, 4))
        val id = requireNotNull(store.preserveAndClearActiveVault())
        val preserved = File(
            context.noBackupFilesDir,
            "database_recovery/$id/database/${DatabaseSchema.DATABASE_NAME}",
        )
        preserved.appendBytes(byteArrayOf(9))

        assertTrue(runCatching { store.verify(id) }.isFailure)
    }

    @Test
    fun legacyV1PackageRemainsRecoverable() {
        val id = "${System.currentTimeMillis()}-${UUID.randomUUID()}"
        val root = File(context.noBackupFilesDir, "database_recovery/$id")
        val database = File(root, "database/${DatabaseSchema.DATABASE_NAME}")
        database.parentFile?.mkdirs()
        database.writeBytes(byteArrayOf(1, 2, 3))
        File(root, "manifest.properties").writeText(
            buildString {
                appendLine("formatVersion=1")
                appendLine("databaseName=${DatabaseSchema.DATABASE_NAME}")
                appendLine("createdAtEpochMs=${System.currentTimeMillis()}")
                appendLine("databaseFiles=${DatabaseSchema.DATABASE_NAME}")
                appendLine("resourceDirectories=")
            },
        )

        assertEquals(id, store.verify(id).info.id)
        assertEquals(DatabaseRecoveryStatus.PENDING_SCAN, store.listPackages().single().status)
    }

    @Test
    fun invalidPackageIsVisibleAndCanBeDeleted() {
        val id = "${System.currentTimeMillis()}-${UUID.randomUUID()}"
        val root = File(context.noBackupFilesDir, "database_recovery/$id")
        root.mkdirs()
        File(root, "manifest.properties").writeText("not-a-manifest")

        val listed = store.listPackages().single()
        assertEquals(DatabaseRecoveryStatus.UNREADABLE, listed.status)
        store.delete(id)
        assertTrue(store.listPackages().isEmpty())
    }

    private fun clean() {
        context.deleteDatabase(DatabaseSchema.DATABASE_NAME)
        VaultResourcePaths.resourceDirectories(context).forEach(File::deleteRecursively)
        File(context.noBackupFilesDir, "database_recovery").deleteRecursively()
        File(context.noBackupFilesDir, "database_recovery_state").deleteRecursively()
    }
}
