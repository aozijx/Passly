package com.aozijx.passly.feature.settings.datamanagement

import com.aozijx.passly.data.database.model.DatabaseRecoveryReport
import com.aozijx.passly.data.database.model.DatabaseRecoveryScan
import com.aozijx.passly.domain.entry.model.EntryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataManagementSettingsReducerTest {

    @Test
    fun `settings updates preserve trash workflow`() {
        val result = DataManagementSettingsReducer.reduce(
            DataManagementSettingsUiState(isEmptyingTrash = true),
            DataManagementSettingsMutation.SettingsChanged(
                autoDownloadIcons = false,
                directoryUri = "content://backup",
            ),
        )

        assertFalse(result.isAutoDownloadIcons)
        assertEquals("content://backup", result.directoryUri)
        assertTrue(result.isEmptyingTrash)
    }

    @Test
    fun `entry operation clears error and owns busy entry`() {
        val started = DataManagementSettingsReducer.reduce(
            DataManagementSettingsUiState(trashError = "old"),
            DataManagementSettingsMutation.TrashEntryActionStarted("entry-1"),
        )
        val finished = DataManagementSettingsReducer.reduce(
            started,
            DataManagementSettingsMutation.TrashEntryActionFinished,
        )

        assertEquals("entry-1", started.activeTrashEntryId)
        assertNull(started.trashError)
        assertNull(finished.activeTrashEntryId)
    }

    @Test
    fun `trash failure keeps current entries`() {
        val result = DataManagementSettingsReducer.reduce(
            DataManagementSettingsUiState(isTrashLoading = true),
            DataManagementSettingsMutation.TrashLoadFailed("failed"),
        )

        assertFalse(result.isTrashLoading)
        assertEquals("failed", result.trashError)
    }

    @Test
    fun `recovery scan selects every recoverable entry type`() {
        val scan = DatabaseRecoveryScan(
            packageId = "package-1",
            recoverableByType = mapOf(EntryType.LOGIN to 2, EntryType.NOTE to 1),
            deletedEntries = 0,
            conflictingEntries = 0,
            damagedEntries = 0,
            recoverableAttachments = 0,
            damagedResources = 0,
            issues = emptyList(),
        )

        val scanned = DataManagementSettingsReducer.reduce(
            DataManagementSettingsUiState(activeRecoveryPackageId = "package-1"),
            DataManagementSettingsMutation.RecoveryScanCompleted(scan),
        )
        val toggled = DataManagementSettingsReducer.reduce(
            scanned,
            DataManagementSettingsMutation.RecoveryTypeToggled(EntryType.NOTE),
        )

        assertEquals(setOf(EntryType.LOGIN, EntryType.NOTE), scanned.selectedRecoveryTypes)
        assertEquals(setOf(EntryType.LOGIN), toggled.selectedRecoveryTypes)
        assertNull(scanned.activeRecoveryPackageId)
    }

    @Test
    fun `recovery completion clears scan and keeps report`() {
        val report = DatabaseRecoveryReport(
            packageId = "package-1",
            restoredEntries = 3,
            skippedConflicts = 1,
            restoredAttachments = 2,
            skippedResources = 0,
            restoredRevisions = 4,
            restoredLinks = 1,
            issues = emptyList(),
        )
        val result = DataManagementSettingsReducer.reduce(
            DataManagementSettingsUiState(
                recoveryScan = DatabaseRecoveryScan(
                    "package-1",
                    mapOf(EntryType.LOGIN to 3),
                    0,
                    1,
                    0,
                    2,
                    0,
                    emptyList(),
                ),
                selectedRecoveryTypes = setOf(EntryType.LOGIN),
            ),
            DataManagementSettingsMutation.RecoveryRestoreCompleted(report),
        )

        assertEquals(report, result.recoveryReport)
        assertNull(result.recoveryScan)
        assertTrue(result.selectedRecoveryTypes.isEmpty())
    }
}
