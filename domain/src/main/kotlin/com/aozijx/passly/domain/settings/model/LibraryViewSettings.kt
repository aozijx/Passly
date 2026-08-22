package com.aozijx.passly.domain.settings.model

import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode

data class LibraryViewSettings(
    val visibleQuickFilters: VisibleQuickFiltersConfig? = null,
    val sort: EntrySort = EntrySort.DEFAULT,
    val entryCardPresentations: List<EntryCardPresentation> = emptyList(),
    val entryHierarchyDisplayMode: EntryHierarchyDisplayMode =
        EntryHierarchyDisplayMode.COLLAPSED
)

data class VisibleQuickFiltersConfig(
    val filterKeys: Set<String>,
    val configured: Boolean = false
)

data class EntryCardPresentation(
    val entryTypeKey: String,
    val variantKey: String = "",
    val density: CardDensity = CardDensity.STANDARD,
    val showIcon: Boolean = true,
    val showFavorite: Boolean = true,
    val showSecondaryText: Boolean = true,
    val showQuickAction: Boolean = true
)

enum class CardDensity { COMPACT, STANDARD, COMFORTABLE }
