package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.data.local.datastore.settings.VaultSortPreference
import com.aozijx.passly.data.local.datastore.settings.VaultViewPreferences
import org.junit.Assert.assertThrows
import org.junit.Test

class LibraryProtoMappingsTest {
    @Test
    fun `unknown hierarchy key is rejected instead of becoming collapsed`() {
        val proto = VaultViewPreferences.newBuilder()
            .setEntryHierarchyDisplayMode("stacked")
            .build()

        assertThrows(IllegalArgumentException::class.java) {
            readVault(proto)
        }
    }

    @Test
    fun `unknown sort field is rejected instead of becoming a valid default`() {
        val proto = VaultViewPreferences.newBuilder()
            .setSort(
                VaultSortPreference.newBuilder()
                    .setField("POPULARITY")
                    .setTieBreakerField("ID"),
            )
            .build()

        assertThrows(IllegalArgumentException::class.java) {
            readVault(proto)
        }
    }
}
