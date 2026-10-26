package com.aozijx.passly.feature.vault.presentation.list

import com.aozijx.passly.feature.vault.model.AddType
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultReducerTest {

    @Test
    fun `closing search also clears its query`() {
        val initial = VaultUiState(searchQuery = "mail", isSearchActive = true)

        val result = VaultReducer.reduce(
            initial,
            VaultMutation.SearchVisibilityChanged(active = false),
        )

        assertFalse(result.isSearchActive)
        assertEquals("", result.searchQuery)
    }

    @Test
    fun `category results do not overwrite interaction state`() {
        val initial = VaultUiState(
            searchQuery = "work",
            isSearchActive = true,
            selectedFilters = setOf(AddType.PASSWORD),
        )

        val result = VaultReducer.reduce(
            initial,
            VaultMutation.CategoriesChanged(listOf("Work")),
        )

        assertEquals("work", result.searchQuery)
        assertTrue(result.isSearchActive)
        assertEquals(setOf(AddType.PASSWORD), result.selectedFilters)
        assertEquals(listOf("Work"), result.availableCategories)
    }

    @Test
    fun `clearing dialogs preserves list and display preferences`() {
        val initial = VaultUiState(
            showTOTPCode = false,
            pendingDelete = EntryListItem(
                EntryIdentity(EntryId("pending"), EntryType.LOGIN, timestamps = EntryTimestamps(1L)),
                EntryProfile(title = "Pending"),
            ),
            availableCategories = listOf("Personal"),
        )

        val result = VaultReducer.reduce(initial, VaultMutation.DialogsCleared)

        assertNull(result.pendingDelete)
        assertFalse(result.showTOTPCode)
        assertEquals(listOf("Personal"), result.availableCategories)
    }

    @Test
    fun `specific filters toggle independently and all clears them`() {
        val initial = VaultUiState(selectedFilters = setOf(AddType.TOTP))

        val combined = VaultReducer.reduce(
            initial,
            VaultMutation.FilterToggled(AddType.PASSWORD),
        )
        val cleared = VaultReducer.reduce(combined, VaultMutation.FilterToggled(null))

        assertEquals(setOf(AddType.TOTP, AddType.PASSWORD), combined.selectedFilters)
        assertEquals(emptySet<AddType>(), cleared.selectedFilters)
    }
}
