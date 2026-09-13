package com.aozijx.passly.presentation.ui.vault.list.component.list

import org.junit.Assert.assertEquals
import org.junit.Test

class VaultListViewportTest {
    @Test
    fun `search uses a transient viewport without replacing the main list anchor`() {
        assertEquals(VaultListViewport.MAIN, resolveVaultListViewport(isSearchActive = false))
        assertEquals(VaultListViewport.SEARCH, resolveVaultListViewport(isSearchActive = true))
    }
}
