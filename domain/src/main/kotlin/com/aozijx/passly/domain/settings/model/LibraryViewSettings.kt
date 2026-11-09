package com.aozijx.passly.domain.settings.model

import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.entry.model.query.EntrySort

data class LibraryViewSettings(
    val sort: EntrySort,
    val entryCardPresentations: List<EntryCardPresentation>,
    val entryHierarchyDisplayMode: EntryHierarchyDisplayMode,
)

data class EntryCardPresentation(
    val entryTypeKey: String,
    val variantKey: String,
    val density: CardDensity,
    val showIcon: Boolean,
    val showFavorite: Boolean,
    val showSecondaryText: Boolean,
    val showQuickAction: Boolean,
)

enum class CardDensity { COMPACT, STANDARD, COMFORTABLE }
