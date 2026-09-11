package com.aozijx.passly.domain.settings.port

import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.settings.model.LibraryViewSettings
import kotlinx.coroutines.flow.Flow

interface LibraryViewSettingsSource {
    val libraryViewSettings: Flow<LibraryViewSettings>
}

interface LibraryViewSettingsRepository : LibraryViewSettingsSource {
    suspend fun setSort(sort: EntrySort)
    suspend fun setEntryHierarchyDisplayMode(mode: EntryHierarchyDisplayMode)
}
