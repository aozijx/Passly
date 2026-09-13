package com.aozijx.passly.presentation.ui.vault.list.component.topbar

import org.junit.Assert.assertEquals
import org.junit.Test

class VaultSearchBarModeTest {
    @Test
    fun `search presentation distinguishes default editing and compact result`() {
        assertEquals(
            VaultSearchBarMode.DEFAULT,
            resolveVaultSearchBarMode(isSearchActive = false, isEditing = false, query = ""),
        )
        assertEquals(
            VaultSearchBarMode.EDITING,
            resolveVaultSearchBarMode(isSearchActive = true, isEditing = true, query = "mail"),
        )
        assertEquals(
            VaultSearchBarMode.RESULT,
            resolveVaultSearchBarMode(isSearchActive = true, isEditing = false, query = "mail"),
        )
    }

    @Test
    fun `pull expansion follows bounded progress and editing stays fully expanded`() {
        assertEquals(
            0.4f,
            resolveVaultSearchBarExpansion(0.4f, VaultSearchBarMode.DEFAULT),
        )
        assertEquals(
            1f,
            resolveVaultSearchBarExpansion(1.4f, VaultSearchBarMode.DEFAULT),
        )
        assertEquals(
            1f,
            resolveVaultSearchBarExpansion(0f, VaultSearchBarMode.EDITING),
        )
        assertEquals(
            0f,
            resolveVaultSearchBarExpansion(-0.2f, VaultSearchBarMode.RESULT),
        )
    }

    @Test
    fun `spring overshoot is clamped before calculating layout dimensions`() {
        assertEquals(1f, clampVaultSearchBarLayoutProgress(1.08f))
        assertEquals(0f, clampVaultSearchBarLayoutProgress(-0.08f))
        assertEquals(0.6f, clampVaultSearchBarLayoutProgress(0.6f))
    }
}
