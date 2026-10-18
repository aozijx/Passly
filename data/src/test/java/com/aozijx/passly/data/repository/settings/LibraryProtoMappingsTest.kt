package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.data.preferences.proto.EntryHierarchyDisplayModePreference
import com.aozijx.passly.data.preferences.proto.VaultSortFieldPreference
import com.aozijx.passly.data.preferences.proto.VaultSortPreference
import com.aozijx.passly.data.preferences.proto.VaultViewPreferences
import com.aozijx.passly.domain.entry.model.query.EntrySortField
import com.aozijx.passly.domain.entry.model.query.SortDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LibraryProtoMappingsTest {
    @Test
    fun `missing stored sort uses newest created entries first`() {
        val settings = readVault(VaultViewPreferences.getDefaultInstance())

        assertEquals(EntrySortField.CREATED_AT, settings.sort.field)
        assertEquals(SortDirection.DESC, settings.sort.direction)
    }

    @Test
    fun `unspecified hierarchy is rejected instead of becoming collapsed`() {
        val proto = VaultViewPreferences.newBuilder()
            .setHierarchyDisplayMode(
                EntryHierarchyDisplayModePreference.ENTRY_HIERARCHY_DISPLAY_MODE_UNSPECIFIED,
            )
            .build()

        assertThrows(IllegalArgumentException::class.java) {
            readVault(proto)
        }
    }

    @Test
    fun `unspecified sort field is rejected instead of becoming a valid default`() {
        val proto = VaultViewPreferences.newBuilder()
            .setSort(
                VaultSortPreference.newBuilder()
                    .setField(VaultSortFieldPreference.VAULT_SORT_FIELD_UNSPECIFIED),
            )
            .build()

        assertThrows(IllegalArgumentException::class.java) {
            readVault(proto)
        }
    }
}
