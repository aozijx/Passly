package com.aozijx.passly.app.database.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aozijx.passly.domain.backup.model.VaultBackupReadOptions
import com.aozijx.passly.domain.backup.model.VaultBackupResource
import com.aozijx.passly.domain.backup.model.VaultBackupResourceKind
import com.aozijx.passly.domain.backup.model.VaultBackupRestoreMode
import com.aozijx.passly.domain.backup.model.VaultBackupRestoreResult
import com.aozijx.passly.domain.backup.model.VaultBackupSnapshot
import com.aozijx.passly.domain.backup.port.VaultBackupStore
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.otp.generation.OtpResult
import com.aozijx.passly.domain.settings.model.OtpRuntimePolicy
import com.aozijx.passly.domain.settings.port.OtpRuntimePolicySnapshotReader
import com.aozijx.passly.feature.backup.internal.archive.snapshot.BackupSnapshotReadOptions
import com.aozijx.passly.feature.vault.otp.OtpCodeRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppBackupSnapshotGatewayTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun readMapsTypedResourcesToExistingStableArchiveIds() =
        runBlocking {
            val iconBytes = byteArrayOf(0x52, 0x49, 0x46, 0x46, 1)
            val attachmentBytes = "attachment".toByteArray()
            val snapshot =
                VaultBackupSnapshot(
                    entries = listOf(account("entry-1")),
                    links = emptyList(),
                    resources =
                        listOf(
                            VaultBackupResource(
                                id = "domain-icon-id",
                                entryId = EntryId("entry-1"),
                                kind = VaultBackupResourceKind.ICON,
                                fileName = "icon.webp",
                                mimeType = "image/webp",
                                createdAt = null,
                                content = iconBytes,
                            ),
                            VaultBackupResource(
                                id = "attachment-1",
                                entryId = EntryId("entry-1"),
                                kind = VaultBackupResourceKind.ATTACHMENT,
                                fileName = "notes.txt",
                                mimeType = "text/plain",
                                createdAt = 5L,
                                content = attachmentBytes,
                            ),
                        ),
                )
            val store = RecordingVaultBackupStore(snapshot)
            val gateway = AppBackupSnapshotGateway(store, context, emptyRuntime())

            val bundle =
                gateway.read(
                    BackupSnapshotReadOptions(
                        includeIcons = true,
                        includeAttachments = true,
                        includeDeleted = true,
                        includedEntryTypes = setOf(EntryType.ACCOUNT),
                    ),
                )

            assertEquals(listOf("icon_entry-1", "attachment-1"), bundle.document.resources.map { it.id })
            assertEquals(
                listOf("attachment-1"),
                bundle.document.entries
                    .single()
                    .attachmentIds,
            )
            assertArrayEquals(iconBytes, bundle.resourceData.getValue("icon_entry-1"))
            assertArrayEquals(attachmentBytes, bundle.resourceData.getValue("attachment-1"))
            assertEquals(16 * 1024 * 1024L, store.readOptions?.maxResourceBytes)
        }

    private fun account(id: String) =
        Entry(
            identity =
                EntryIdentity(
                    id = EntryId(id),
                    type = EntryType.ACCOUNT,
                    version = EntryVersion.INITIAL,
                    timestamps = EntryTimestamps(createdAtMs = 1L, updatedAtMs = 2L),
                ),
            profile = EntryProfile("Account"),
        )

    private fun emptyRuntime() =
        OtpCodeRuntime(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
            otpRuntimePolicyReader =
                object : OtpRuntimePolicySnapshotReader {
                    override suspend fun readOtpRuntimePolicySnapshot() = OtpRuntimePolicy()
                },
            codeGenerator = { _, _ -> OtpResult.Success("unused") },
            loadOtpMaterial = { null },
        )

    private class RecordingVaultBackupStore(
        private val snapshot: VaultBackupSnapshot,
    ) : VaultBackupStore {
        var readOptions: VaultBackupReadOptions? = null

        override suspend fun read(options: VaultBackupReadOptions): VaultBackupSnapshot {
            readOptions = options
            return snapshot
        }

        override suspend fun restore(
            snapshot: VaultBackupSnapshot,
            mode: VaultBackupRestoreMode,
            onCommitted: suspend () -> Unit,
        ): VaultBackupRestoreResult {
            onCommitted()
            return VaultBackupRestoreResult(0, 0)
        }
    }
}
