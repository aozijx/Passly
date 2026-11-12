package com.aozijx.passly.app.database.backup

import android.content.ContextWrapper
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
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.otp.generation.OtpResult
import com.aozijx.passly.feature.backup.contract.ImportMode
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.archive.model.BackupDocument
import com.aozijx.passly.feature.backup.internal.archive.snapshot.BackupSnapshotReadOptions
import com.aozijx.passly.feature.vault.otp.OtpCodeRuntime
import com.aozijx.passly.feature.vault.otp.OtpCodeState
import com.aozijx.passly.feature.vault.otp.OtpRuntimeInvalidator
import com.aozijx.passly.feature.vault.otp.defaultOtpRuntimePolicyReader
import com.aozijx.passly.feature.vault.otp.generationMaterialForTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class AppBackupSnapshotGatewayTest {
    @Test
    fun restoreMapsStableIconKeyToDomainResourceAndTransfersItsBytes() =
        runTest {
            val entry = account("entry")
            val content = byteArrayOf(3, 4, 5)
            val bundle =
                BackupSnapshotArchiveMapper.toBundle(
                    snapshot =
                        VaultBackupSnapshot(
                            entries = listOf(entry),
                            links = emptyList(),
                            resources = listOf(icon(entry.id, content)),
                        ),
                    appVersion = { "1.0" },
                    exportedAt = 10L,
                )
            val archiveResource = bundle.document.resources.single()
            assertEquals("icon_entry", archiveResource.id)

            val store = RecordingVaultBackupStore()
            val gateway = AppBackupSnapshotGateway(store, ContextWrapper(null), emptyRuntime())
            val result = gateway.restore(bundle, ImportMode.APPEND)

            assertEquals(1, result.importedEntryCount)
            assertEquals(0, result.existingEntryCount)
            assertEquals(VaultBackupRestoreMode.APPEND, store.restoredMode)
            val restored = requireNotNull(store.restoredSnapshot)
            assertEquals("icon:entry", restored.resources.single().id)
            assertSame(content, restored.resources.single().content)
            assertArrayEquals(byteArrayOf(3, 4, 5), restored.resources.single().content)
        }

    @Test
    fun overwriteWithCompatibilityFilteredEntriesNeverCallsStore() =
        runTest {
            val bundle =
                BackupBundle(
                    document =
                        BackupDocument(
                            format = BackupDocument.FORMAT,
                            version = BackupDocument.CURRENT_VERSION,
                            exportedAt = 10L,
                            entries = emptyList(),
                        ),
                    sourceEntryCount = 1,
                )
            val store = RecordingVaultBackupStore()
            val gateway = AppBackupSnapshotGateway(store, ContextWrapper(null), emptyRuntime())

            try {
                gateway.restore(bundle, ImportMode.OVERWRITE)
                fail("Expected overwrite protection to reject a filtered nonempty source")
            } catch (_: IllegalArgumentException) {
                // Expected: this guard runs before validation or persistence.
            }

            assertEquals(0, store.restoreCalls)
            assertEquals(null, store.restoredSnapshot)
        }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun overwriteReloadsEveryEntryStillLeasedByTheRuntime() =
        runTest {
            var loads = 0
            var otpSecret = "JBSWY3DPEHPK3PXP"
            val loadedSeeds = mutableListOf<String>()
            val runtime =
                OtpCodeRuntime(
                    scope = backgroundScope,
                    otpRuntimePolicyReader = defaultOtpRuntimePolicyReader(),
                    codeGenerator = { material, _ ->
                        loadedSeeds += String(material.secretBytes, Charsets.UTF_8)
                        OtpResult.Success("code-${loadedSeeds.size}")
                    },
                    loadOtpMaterial = {
                        loads++
                        generationMaterialForTest(OtpConfig.Totp(secret = otpSecret))
                    },
                )
            val lease = runtime.acquire("active-entry")
            runCurrent()
            assertEquals(1, loads)
            val entry = account("active-entry")
            val bundle =
                BackupSnapshotArchiveMapper.toBundle(
                    snapshot = VaultBackupSnapshot(entries = listOf(entry), links = emptyList(), resources = emptyList()),
                    appVersion = { "1.0" },
                    exportedAt = 10L,
                )
            val store = RecordingVaultBackupStore()
            lateinit var restoreJob: Job
            store.afterPersist = {
                otpSecret = "MFRGGZDFMZTWQ2LK"
                restoreJob.cancel()
            }
            val invalidator: OtpRuntimeInvalidator = runtime
            val gateway = AppBackupSnapshotGateway(store, ContextWrapper(null), invalidator)

            restoreJob = launch { gateway.restore(bundle, ImportMode.OVERWRITE) }
            restoreJob.join()
            assertEquals(2, loads)
            assertEquals(
                listOf(
                    "JBSWY3DPEHPK3PXP",
                    "JBSWY3DPEHPK3PXP",
                    "MFRGGZDFMZTWQ2LK",
                    "MFRGGZDFMZTWQ2LK",
                ),
                loadedSeeds,
            )
            val activeState = runtime.states.value["active-entry"] as? OtpCodeState.TimedReady
            assertEquals("code-3", activeState?.code)
            lease.close()
        }

    private fun emptyRuntime() = OtpRuntimeInvalidator {}

    private fun icon(
        entryId: EntryId,
        content: ByteArray,
    ) = VaultBackupResource(
        id = "icon:${entryId.value}",
        entryId = entryId,
        kind = VaultBackupResourceKind.ICON,
        fileName = "icon.webp",
        mimeType = "image/webp",
        createdAt = null,
        content = content,
    )

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

    private class RecordingVaultBackupStore : VaultBackupStore {
        var restoredSnapshot: VaultBackupSnapshot? = null
        var restoredMode: VaultBackupRestoreMode? = null
        var restoreCalls: Int = 0
        var afterPersist: (() -> Unit)? = null

        override suspend fun read(options: VaultBackupReadOptions): VaultBackupSnapshot = error("read is not used by these tests")

        override suspend fun restore(
            snapshot: VaultBackupSnapshot,
            mode: VaultBackupRestoreMode,
            onCommitted: suspend () -> Unit,
        ): VaultBackupRestoreResult {
            restoreCalls++
            restoredSnapshot = snapshot
            restoredMode = mode
            afterPersist?.invoke()
            onCommitted()
            return VaultBackupRestoreResult(importedEntryCount = snapshot.entries.size, existingEntryCount = 0)
        }
    }
}
