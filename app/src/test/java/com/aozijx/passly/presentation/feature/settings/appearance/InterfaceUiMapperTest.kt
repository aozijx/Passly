package com.aozijx.passly.presentation.feature.settings.appearance

import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.settings.model.LibraryQuickFilter
import com.aozijx.passly.presentation.ui.settings.appearance.model.LibraryQuickFilterUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class InterfaceUiMapperTest {
    @Test
    fun hierarchyModesRoundTripAcrossFeatureUiBoundary() {
        EntryHierarchyDisplayMode.entries.forEach { mode ->
            assertEquals(mode, mode.toUiModel().toDomainModel())
        }
    }

    @Test
    fun quickFilterOptionsExposeSelectionWithoutDomainTypes() {
        val options = libraryQuickFilterOptions(setOf(LibraryQuickFilter.TOTP.settingsKey))

        assertEquals(
            listOf(false, true),
            options.map { it.selected },
        )
        assertEquals(
            LibraryQuickFilter.PASSWORDS,
            LibraryQuickFilterUiModel.PASSWORDS.toDomainModel(),
        )
        assertEquals(
            LibraryQuickFilter.TOTP,
            LibraryQuickFilterUiModel.TOTP.toDomainModel(),
        )
    }
}
