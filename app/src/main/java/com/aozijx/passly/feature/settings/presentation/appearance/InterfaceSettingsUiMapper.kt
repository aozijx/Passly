package com.aozijx.passly.feature.settings.presentation.appearance

import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.settings.model.AppCornerRadiusConstraints
import com.aozijx.passly.domain.settings.model.InterfaceSettings
import com.aozijx.passly.domain.settings.model.LibraryViewSettings
import com.aozijx.passly.domain.settings.model.OtpRuntimePolicy

internal object InterfaceSettingsUiMapper {
    fun map(
        settings: InterfaceSettings,
        librarySettings: LibraryViewSettings,
        otpPolicy: OtpRuntimePolicy,
    ): InterfaceUiState =
        InterfaceUiState(
            hideSystemBars = settings.hideSystemBars,
            collapseTopBarOnScroll = settings.collapseTopBarOnScroll,
            collapseQuickFilterBarOnScroll = settings.collapseQuickFilterBarOnScroll,
            appCornerRadiusDp = settings.appCornerRadiusDp,
            appCornerRadiusRange =
                AppCornerRadiusConstraints.MIN_DP..AppCornerRadiusConstraints.MAX_DP,
            entryHierarchyDisplayMode = librarySettings.entryHierarchyDisplayMode,
            entryHierarchyDisplayModeLabelRes = librarySettings.entryHierarchyDisplayMode.labelRes(),
            entryHierarchyDisplayModeOptions =
                EntryHierarchyDisplayMode.entries.map { mode ->
                    EntryHierarchyDisplayModeOptionUiState(mode, mode.labelRes())
                },
            cardStylePresentations = librarySettings.entryCardPresentations,
            copyNextTotpNearExpiry = otpPolicy.copyNextTotpNearExpiry,
        )

    private fun EntryHierarchyDisplayMode.labelRes(): Int =
        when (this) {
            EntryHierarchyDisplayMode.COLLAPSED -> R.string.settings_interface_entry_hierarchy_collapsed
            EntryHierarchyDisplayMode.EXPANDED -> R.string.settings_interface_entry_hierarchy_expanded
            EntryHierarchyDisplayMode.SEPARATE -> R.string.settings_interface_entry_hierarchy_separate
        }
}
