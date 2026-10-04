package com.aozijx.passly.feature.backup.internal.archive

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
import com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupArchiveKeyRegistryTest {
    @Test
    fun entryTypeKeysAreExplicitStableAndComplete() {
        assertEquals("login", BackupArchiveKeyRegistry.entryTypeKey(EntryType.LOGIN))
        assertEquals("bank_card", BackupArchiveKeyRegistry.entryTypeKey(EntryType.BANK_CARD))
        assertEquals("id_card", BackupArchiveKeyRegistry.entryTypeKey(EntryType.ID_CARD))
        EntryType.entries.forEach { type ->
            assertEquals(type, BackupArchiveKeyRegistry.entryType(BackupArchiveKeyRegistry.entryTypeKey(type)))
        }
        assertEquals(EntryType.entries.toSet(), BackupArchiveKeyRegistry.archiveEntryTypes)
        assertNull(BackupArchiveKeyRegistry.entryType("LOGIN"))
    }

    @Test
    fun fieldKeysAreExplicitStableAndComplete() {
        assertEquals("title", BackupArchiveKeyRegistry.fieldKey(FieldKey.TITLE))
        assertEquals("card_number", BackupArchiveKeyRegistry.fieldKey(FieldKey.CARD_NUMBER))
        FieldKey.entries.forEach { field ->
            assertEquals(field, BackupArchiveKeyRegistry.field(BackupArchiveKeyRegistry.fieldKey(field)))
        }
        assertNull(BackupArchiveKeyRegistry.field("PASSWORD"))
    }

    @Test
    fun relationAndResourceKeysDoNotUseEnumNames() {
        EntryRelationType.entries.forEach { type ->
            assertEquals(
                type,
                BackupArchiveKeyRegistry.relationType(BackupArchiveKeyRegistry.relationTypeKey(type)),
            )
        }
        BackupResourceKind.entries.forEach { kind ->
            assertEquals(
                kind,
                BackupArchiveKeyRegistry.resourceKind(BackupArchiveKeyRegistry.resourceKindKey(kind)),
            )
        }
        assertNull(BackupArchiveKeyRegistry.relationType("OTP_FOR"))
        assertNull(BackupArchiveKeyRegistry.resourceKind("ATTACHMENT"))
    }
}
