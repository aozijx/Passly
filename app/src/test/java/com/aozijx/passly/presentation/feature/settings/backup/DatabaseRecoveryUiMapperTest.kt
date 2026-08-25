package com.aozijx.passly.presentation.feature.settings.backup

import com.aozijx.passly.data.local.database.model.DatabaseRecoveryPackage
import com.aozijx.passly.data.local.database.model.DatabaseRecoveryScan
import com.aozijx.passly.data.local.database.model.DatabaseRecoveryStatus
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.presentation.ui.settings.backup.DatabaseRecoveryPackageStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class DatabaseRecoveryUiMapperTest {

    @Test
    fun `maps data and domain recovery models to ui-only snapshot`() {
        val state = DatabaseRecoveryUiState(
            recoveryPackages = listOf(
                DatabaseRecoveryPackage(
                    id = "package-1",
                    createdAtEpochMs = 123L,
                    sizeBytes = 456L,
                    status = DatabaseRecoveryStatus.PARTIALLY_RECOVERABLE,
                ),
            ),
            isRecoveryLoading = false,
            recoveryScan = DatabaseRecoveryScan(
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
        )

        val result = state.toSheetState()

        assertEquals(DatabaseRecoveryPackageStatus.PARTIALLY_RECOVERABLE, result.packages.single().status)
        assertEquals(listOf("LOGIN", "NOTE"), result.scan?.recoverableTypes?.map { it.id })
        assertEquals(listOf("LOGIN", "NOTE"), result.scan?.recoverableTypes?.map { it.label })
        assertEquals(3, result.scan?.recoverableEntries)
        assertEquals(setOf("NOTE"), result.selectedTypeIds)
    }
}
