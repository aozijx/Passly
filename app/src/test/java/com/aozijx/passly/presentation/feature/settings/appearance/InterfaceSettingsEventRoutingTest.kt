package com.aozijx.passly.presentation.feature.settings.appearance

import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.presentation.feature.settings.ui.appearance.model.EntryHierarchyDisplayModeUiModel
import com.aozijx.passly.presentation.feature.settings.ui.appearance.model.InterfaceSettingsEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class InterfaceSettingsEventRoutingTest {
    @Test
    fun `interface events preserve their values when routed to feature actions`() {
        val cases = listOf(
            InterfaceSettingsEvent.StatusBarAutoHideChanged(true) to
                InterfaceSettingsAction.SetHideSystemBars(true),
            InterfaceSettingsEvent.TopBarCollapsibleChanged(false) to
                InterfaceSettingsAction.SetTopBarCollapsible(false),
            InterfaceSettingsEvent.QuickFilterBarCollapsibleChanged(true) to
                InterfaceSettingsAction.SetQuickFilterBarCollapsible(true),
            InterfaceSettingsEvent.AppCornerRadiusChanged(28f) to
                InterfaceSettingsAction.SetAppCornerRadius(28f),
            InterfaceSettingsEvent.EntryHierarchyDisplayModeChanged(
                EntryHierarchyDisplayModeUiModel.SEPARATE,
            ) to InterfaceSettingsAction.SetEntryHierarchyDisplayMode(
                EntryHierarchyDisplayMode.SEPARATE,
            ),
        )

        cases.forEach { (event, expectedAction) ->
            assertEquals(expectedAction, event.toAction())
        }
    }
}
