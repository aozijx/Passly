package com.aozijx.passly.feature.vault.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultQuickFilterTest {

    @Test
    fun defaultsOnlyShowRequiredQuickFilters() {
        assertEquals(
            setOf(VaultQuickFilter.ALL.settingsKey),
            VaultQuickFilter.defaultVisibleKeys
        )
    }

    @Test
    fun allQuickFilterCannotBeRemovedBySettings() {
        assertFalse(VaultQuickFilter.ALL.isToggleable)
        assertTrue(VaultQuickFilter.ALL in VaultQuickFilter.resolveVisible(emptySet()))
    }

    @Test
    fun toggleVisibleKeyKeepsRequiredFiltersAndTogglesSelectedFilter() {
        val withPasswords = VaultQuickFilter.toggleVisibleKey(
            enabledKeys = VaultQuickFilter.defaultVisibleKeys,
            quickFilter = VaultQuickFilter.PASSWORDS
        )

        assertEquals(setOf("all", "passwords"), withPasswords)

        val withoutPasswords = VaultQuickFilter.toggleVisibleKey(
            enabledKeys = withPasswords,
            quickFilter = VaultQuickFilter.PASSWORDS
        )

        assertEquals(setOf("all"), withoutPasswords)
    }
}
