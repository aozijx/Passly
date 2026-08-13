package com.aozijx.passly.feature.vault.model

import com.aozijx.passly.domain.settings.model.LibraryQuickFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryQuickFilterTest {

    @Test
    fun defaultsOnlyShowRequiredQuickFilters() {
        assertEquals(
            setOf(LibraryQuickFilter.ALL.settingsKey),
            LibraryQuickFilter.defaultVisibleKeys
        )
    }

    @Test
    fun allQuickFilterCannotBeRemovedBySettings() {
        assertFalse(LibraryQuickFilter.ALL.isToggleable)
        assertTrue(LibraryQuickFilter.ALL in LibraryQuickFilter.resolveVisible(emptySet()))
    }

    @Test
    fun toggleVisibleKeyKeepsRequiredFiltersAndTogglesSelectedFilter() {
        val withPasswords = LibraryQuickFilter.toggleVisibleKey(
            enabledKeys = LibraryQuickFilter.defaultVisibleKeys,
            quickFilter = LibraryQuickFilter.PASSWORDS
        )

        assertEquals(setOf("all", "passwords"), withPasswords)

        val withoutPasswords = LibraryQuickFilter.toggleVisibleKey(
            enabledKeys = withPasswords,
            quickFilter = LibraryQuickFilter.PASSWORDS
        )

        assertEquals(setOf("all"), withoutPasswords)
    }
}
