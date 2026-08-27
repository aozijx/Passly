package com.aozijx.passly.presentation.feature.database.recovery

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.database.recovery.RecoverableDatabaseReport
import com.aozijx.passly.feature.database.recovery.RecoverableDatabaseScan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseRecoveryReducerTest {

    @Test
    fun `database clear lifecycle stays inside recovery state`() {
        val started = DatabaseRecoveryReducer.reduce(
            DatabaseRecoveryUiState(databaseCleared = true),
            DatabaseRecoveryMutation.DatabaseClearStarted,
        )
        val completed = DatabaseRecoveryReducer.reduce(
            started,
            DatabaseRecoveryMutation.DatabaseClearCompleted,
        )

        assertTrue(started.isClearingDatabase)
        assertFalse(started.databaseCleared)
        assertFalse(completed.isClearingDatabase)
        assertTrue(completed.databaseCleared)
    }

    @Test
    fun `database clear terminal paths always release busy state`() {
        val clearing = DatabaseRecoveryUiState(isClearingDatabase = true)

        val failed = DatabaseRecoveryReducer.reduce(
            clearing,
            DatabaseRecoveryMutation.RecoveryOperationFailed("failed"),
        )
        val cancelled = DatabaseRecoveryReducer.reduce(
            clearing,
            DatabaseRecoveryMutation.RecoveryResultCleared,
        )

        assertFalse(failed.isClearingDatabase)
        assertFalse(cancelled.isClearingDatabase)
    }

    @Test
    fun `recovery scan selects every recoverable entry type`() {
        val scan = RecoverableDatabaseScan(
            packageId = "package-1",
            recoverableByType = mapOf(EntryType.LOGIN to 2, EntryType.NOTE to 1),
            deletedEntries = 0,
            conflictingEntries = 0,
            damagedEntries = 0,
            recoverableAttachments = 0,
            damagedResources = 0,
            issues = emptyList(),
        )

        val scanned = DatabaseRecoveryReducer.reduce(
            DatabaseRecoveryUiState(activeRecoveryPackageId = "package-1"),
            DatabaseRecoveryMutation.RecoveryScanCompleted(scan),
        )
        val toggled = DatabaseRecoveryReducer.reduce(
            scanned,
            DatabaseRecoveryMutation.RecoveryTypeToggled(EntryType.NOTE),
        )

        assertEquals(setOf(EntryType.LOGIN, EntryType.NOTE), scanned.selectedRecoveryTypes)
        assertEquals(setOf(EntryType.LOGIN), toggled.selectedRecoveryTypes)
        assertNull(scanned.activeRecoveryPackageId)
    }

    @Test
    fun `recovery completion clears scan and keeps report`() {
        val report = RecoverableDatabaseReport(
            packageId = "package-1",
            restoredEntries = 3,
            skippedConflicts = 1,
            restoredAttachments = 2,
            skippedResources = 0,
            restoredRevisions = 4,
            restoredLinks = 1,
            issues = emptyList(),
        )
        val result = DatabaseRecoveryReducer.reduce(
            DatabaseRecoveryUiState(
                recoveryScan = RecoverableDatabaseScan(
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
            DatabaseRecoveryMutation.RecoveryRestoreCompleted(report),
        )

        assertEquals(report, result.recoveryReport)
        assertNull(result.recoveryScan)
        assertTrue(result.selectedRecoveryTypes.isEmpty())
    }
}
