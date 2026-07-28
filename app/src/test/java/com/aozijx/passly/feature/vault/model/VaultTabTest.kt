package com.aozijx.passly.feature.vault.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultTabTest {

    @Test
    fun defaultsShowEverySupportedTab() {
        assertEquals(
            VaultTab.entries.map(VaultTab::settingsKey).toSet(),
            VaultTab.defaultVisibleKeys
        )
    }

    @Test
    fun allTabCannotBeRemovedBySettings() {
        assertFalse(VaultTab.ALL.isToggleable)
        assertTrue(VaultTab.ALL in VaultTab.resolveVisible(emptySet()))
    }
}
