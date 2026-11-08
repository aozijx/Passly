package com.aozijx.passly.feature.settings.presentation.appearance

import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.settings.model.EntryCardPresentation

sealed interface InterfaceSettingsAction {
    data object RetryRead : InterfaceSettingsAction

    data class SetHideSystemBars(
        val enabled: Boolean,
    ) : InterfaceSettingsAction

    data class SetTopBarCollapsible(
        val enabled: Boolean,
    ) : InterfaceSettingsAction

    data class SetQuickFilterBarCollapsible(
        val enabled: Boolean,
    ) : InterfaceSettingsAction

    data class SetAppCornerRadius(
        val radiusDp: Float,
    ) : InterfaceSettingsAction

    data class SetEntryHierarchyDisplayMode(
        val mode: EntryHierarchyDisplayMode,
    ) : InterfaceSettingsAction

    data class SetEntryCardPresentation(
        val presentation: EntryCardPresentation,
    ) : InterfaceSettingsAction
}
