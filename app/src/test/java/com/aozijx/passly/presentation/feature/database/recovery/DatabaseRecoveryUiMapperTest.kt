package com.aozijx.passly.presentation.feature.database.recovery

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.database.recovery.RecoverableDatabasePackage
import com.aozijx.passly.feature.database.recovery.RecoverableDatabaseScan
import com.aozijx.passly.feature.database.recovery.RecoverableDatabaseStatus
import com.aozijx.passly.presentation.ui.database.recovery.model.DatabaseRecoveryPackageStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class DatabaseRecoveryUiMapperTest {

    @Test
    fun `maps data and domain recovery models to ui-only snapshot`() {
        val state = DatabaseRecoveryUiState(
            recoveryPackages = listOf(
                RecoverableDatabasePackage(
                    id = "package-1",
                    createdAtMillis = 123L,
                    sizeBytes = 456L,
                    status = RecoverableDatabaseStatus.PARTIALLY_RECOVERABLE,
                ),
            ),
            isRecoveryLoading = false,
            activeRecoveryPackageId = "package-1",
            recoveryScan = RecoverableDatabaseScan(
                packageId = "package-1",
                recoverableByType = linkedMapOf(EntryType.LOGIN to 2, EntryType.NOTE to 1),
                deletedEntries = 0,
                conflictingEntries = 1,
                damagedEntries = 2,
                recoverableAttachments = 3,
                damagedResources = 0,
                issues = emptyList(),
            ),
            selectedRecoveryTypes = setOf(EntryType.NOTE),
            recoveryError = "damaged package",
            isClearingDatabase = true,
            databaseCleared = true,
        )

        val result = state.toSheetState()

        assertEquals(DatabaseRecoveryPackageStatus.PARTIALLY_RECOVERABLE, result.packages.single().status)
        assertEquals(listOf("LOGIN", "NOTE"), result.scan?.recoverableTypes?.map { it.id })
        assertEquals(listOf("LOGIN", "NOTE"), result.scan?.recoverableTypes?.map { it.label })
        assertEquals(3, result.scan?.recoverableEntries)
        assertEquals(setOf("NOTE"), result.selectedTypeIds)
        assertEquals("package-1", result.activePackageId)
        assertEquals("damaged package", result.error)
        assertEquals(true, result.isClearingDatabase)
        assertEquals(true, result.databaseCleared)
        assertEquals(true, result.isBusy)
    }
}
