package com.aozijx.passly.presentation.feature.settings.appearance

import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import org.junit.Assert.assertEquals
import org.junit.Test

class InterfaceUiMapperTest {
    @Test
    fun `ready preferences map to the interface screen`() {
        val model = InterfaceSettingsUiState.Ready(
            hideSystemBars = true,
            collapseTopBarOnScroll = false,
            collapseQuickFilterBarOnScroll = true,
            appCornerRadiusDp = 24f,
            entryHierarchyDisplayMode = EntryHierarchyDisplayMode.EXPANDED,
        ).toInterfaceUiModel()

        assertEquals(true, model.hideSystemBars)
        assertEquals(false, model.collapseTopBarOnScroll)
        assertEquals(true, model.collapseQuickFilterBarOnScroll)
        assertEquals(24f, model.appCornerRadiusDp)
        assertEquals(
            EntryHierarchyDisplayMode.EXPANDED,
            model.entryHierarchyDisplayMode,
        )
        assertEquals(0f..48f, model.appCornerRadiusRange)
    }
}
