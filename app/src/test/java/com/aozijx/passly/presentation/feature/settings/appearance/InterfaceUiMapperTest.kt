package com.aozijx.passly.presentation.feature.settings.appearance

import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import org.junit.Assert.assertEquals
import org.junit.Test

class InterfaceUiMapperTest {
    @Test
    fun hierarchyModesRoundTripAcrossFeatureUiBoundary() {
        EntryHierarchyDisplayMode.entries.forEach { mode ->
            assertEquals(mode, mode.toUiModel().toDomainModel())
        }
    }
}
