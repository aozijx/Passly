package com.aozijx.passly.feature.settings.datamanagement

import com.aozijx.passly.data.local.database.model.DatabaseRecoveryReport
import com.aozijx.passly.data.local.database.model.DatabaseRecoveryScan
import com.aozijx.passly.domain.entry.model.EntryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseRecoveryReducerTest {

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
        val result = DatabaseRecoveryReducer.reduce(
            DatabaseRecoveryUiState(
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
            DatabaseRecoveryMutation.RecoveryRestoreCompleted(report),
        )

        assertEquals(report, result.recoveryReport)
        assertNull(result.recoveryScan)
        assertTrue(result.selectedRecoveryTypes.isEmpty())
    }
}
