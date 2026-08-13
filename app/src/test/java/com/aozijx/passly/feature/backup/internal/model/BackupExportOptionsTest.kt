package com.aozijx.passly.feature.backup.internal.model

import com.aozijx.passly.domain.entry.model.EntryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupExportOptionsTest {

    @Test
    fun defaultsProduceACompleteVaultBackup() {
        val options = BackupExportOptions()

        assertTrue(options.includeIcons)
        assertTrue(options.includeAttachments)
        assertTrue(options.includeDeleted)
        assertEquals(EntryType.entries.toSet(), options.includedEntryTypes)
    }

    @Test
    fun optionsAllowResourcesDeletedEntriesAndTypesToBeExcluded() {
        val selectedTypes = setOf(EntryType.LOGIN, EntryType.OTP)
        val options = BackupExportOptions(
            includeIcons = false,
            includeAttachments = false,
            includeDeleted = false,
            includedEntryTypes = selectedTypes
        )

        assertFalse(options.includeIcons)
        assertFalse(options.includeAttachments)
        assertFalse(options.includeDeleted)
        assertEquals(selectedTypes, options.includedEntryTypes)
    }

    @Test
    fun emptyEntryTypeSelectionIsRejectedAtTheDomainBoundary() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupExportOptions(includedEntryTypes = emptySet())
        }
    }
}
