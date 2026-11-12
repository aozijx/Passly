package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.data.preferences.proto.EntryCardPresentationPreference
import com.aozijx.passly.data.preferences.proto.EntryHierarchyDisplayModePreference
import com.aozijx.passly.data.preferences.proto.VaultSortFieldPreference
import com.aozijx.passly.data.preferences.proto.VaultSortPreference
import com.aozijx.passly.data.preferences.proto.VaultViewPreferences
import com.aozijx.passly.domain.entry.model.query.EntrySortField
import com.aozijx.passly.domain.entry.model.query.SortDirection
import com.aozijx.passly.domain.settings.model.CardDensity
import com.aozijx.passly.domain.settings.model.EntryCardPresentation
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
    fun `card presentations round trip with stable keys and explicit density`() {
        val presentation =
            EntryCardPresentation(
                entryTypeKey = "login",
                variantKey = "password",
                density = CardDensity.COMFORTABLE,
                showIcon = false,
                showFavorite = true,
                showSecondaryText = false,
                showQuickAction = true,
            )
        val proto =
            VaultViewPreferences
                .newBuilder()
                .addEntryCardPresentations(presentation.toProto())
                .build()

        assertEquals(listOf(presentation), readVault(proto).entryCardPresentations)
    }

    @Test
    fun `upserting card presentation replaces same type and keeps other types`() {
        val other = presentation("otp", "totp")
        val oldLogin = presentation("login", "default")
        val selectedLogin = presentation("login", "password")

        assertEquals(
            listOf(selectedLogin, other),
            listOf(oldLogin, other).upsert(selectedLogin),
        )
    }

    private fun presentation(
        entryTypeKey: String,
        variantKey: String,
    ) = EntryCardPresentation(
        entryTypeKey = entryTypeKey,
        variantKey = variantKey,
        density = CardDensity.STANDARD,
        showIcon = true,
        showFavorite = true,
        showSecondaryText = true,
        showQuickAction = true,
    )

    @Test
    fun `unspecified hierarchy is rejected instead of becoming collapsed`() {
        val proto =
            VaultViewPreferences
                .newBuilder()
                .setHierarchyDisplayMode(
                    EntryHierarchyDisplayModePreference.ENTRY_HIERARCHY_DISPLAY_MODE_UNSPECIFIED,
                ).build()

        assertThrows(IllegalArgumentException::class.java) {
            readVault(proto)
        }
    }

    @Test
    fun `unspecified sort field is rejected instead of becoming a valid default`() {
        val proto =
            VaultViewPreferences
                .newBuilder()
                .setSort(
                    VaultSortPreference
                        .newBuilder()
                        .setField(VaultSortFieldPreference.VAULT_SORT_FIELD_UNSPECIFIED),
                ).build()

        assertThrows(IllegalArgumentException::class.java) {
            readVault(proto)
        }
    }
}
