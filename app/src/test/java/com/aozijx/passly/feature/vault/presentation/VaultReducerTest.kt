package com.aozijx.passly.feature.vault.presentation

import com.aozijx.passly.domain.settings.model.LibraryQuickFilter
import com.aozijx.passly.feature.vault.contract.VaultUiState
import com.aozijx.passly.feature.vault.model.AddType
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
    fun `list results do not overwrite interaction state`() {
        val initial = VaultUiState(
            searchQuery = "work",
            isSearchActive = true,
            addType = AddType.PASSWORD,
        )

        val result = VaultReducer.reduce(
            initial,
            VaultMutation.ListChanged(
                isLoading = false,
                categories = listOf("Work"),
                items = emptyList(),
            ),
        )

        assertEquals("work", result.searchQuery)
        assertTrue(result.isSearchActive)
        assertEquals(AddType.PASSWORD, result.addType)
        assertFalse(result.isVaultItemsLoading)
        assertEquals(listOf("Work"), result.availableCategories)
    }

    @Test
    fun `clearing dialogs preserves list and display preferences`() {
        val initial = VaultUiState(
            showTOTPCode = false,
            addType = AddType.TOTP,
            availableCategories = listOf("Personal"),
        )

        val result = VaultReducer.reduce(initial, VaultMutation.DialogsCleared)

        assertNull(result.addType)
        assertFalse(result.showTOTPCode)
        assertEquals(listOf("Personal"), result.availableCategories)
    }

    @Test
    fun `quick filter settings update only visible filters`() {
        val initial = VaultUiState(selectedQuickFilter = LibraryQuickFilter.TOTP)

        val result = VaultReducer.reduce(
            initial,
            VaultMutation.VisibleQuickFiltersChanged(listOf(LibraryQuickFilter.ALL)),
        )

        assertEquals(LibraryQuickFilter.TOTP, result.selectedQuickFilter)
        assertEquals(listOf(LibraryQuickFilter.ALL), result.visibleQuickFilters)
    }
}
