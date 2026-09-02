package com.aozijx.passly.domain.settings.port

import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.settings.model.InterfaceSettings
import kotlinx.coroutines.flow.Flow

data class InterfaceSettingsSnapshot(
    val preferences: InterfaceSettings,
    val visibleLibraryQuickFilterKeys: Set<String>?,
    val entryHierarchyDisplayMode: EntryHierarchyDisplayMode,
)

interface InterfaceSettingsRepository {
    val interfaceSettings: Flow<InterfaceSettingsSnapshot>

    suspend fun setHideSystemBars(enabled: Boolean)
    suspend fun setTopBarCollapsible(enabled: Boolean)
    suspend fun setQuickFilterBarCollapsible(enabled: Boolean)
    suspend fun setOuterCornerRadius(radiusDp: Float)
    suspend fun setInnerCornerRadius(radiusDp: Float)
    suspend fun setGroupItemSpacing(spacingDp: Float)
    suspend fun setGroupContentPadding(paddingDp: Float)
    suspend fun setVisibleLibraryQuickFilters(keys: Set<String>)
    suspend fun setEntryHierarchyDisplayMode(mode: EntryHierarchyDisplayMode)
}
