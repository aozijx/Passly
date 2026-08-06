package com.aozijx.passly.feature.vault.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultTabTest {

    @Test
    fun defaultsOnlyShowRequiredTabs() {
        assertEquals(
            setOf(VaultTab.ALL.settingsKey),
            VaultTab.defaultVisibleKeys
        )
    }

    @Test
    fun allTabCannotBeRemovedBySettings() {
        assertFalse(VaultTab.ALL.isToggleable)
        assertTrue(VaultTab.ALL in VaultTab.resolveVisible(emptySet()))
    }

    @Test
    fun toggleVisibleKeyKeepsRequiredTabsAndTogglesSelectedTab() {
        val withPasswords = VaultTab.toggleVisibleKey(
            enabledKeys = VaultTab.defaultVisibleKeys,
            tab = VaultTab.PASSWORDS
        )

        assertEquals(setOf("all", "passwords"), withPasswords)

        val withoutPasswords = VaultTab.toggleVisibleKey(
            enabledKeys = withPasswords,
            tab = VaultTab.PASSWORDS
        )

        assertEquals(setOf("all"), withoutPasswords)
    }
}
