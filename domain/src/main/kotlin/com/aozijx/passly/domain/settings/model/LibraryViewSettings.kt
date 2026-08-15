package com.aozijx.passly.domain.settings.model

data class LibraryViewSettings(
    val visibleQuickFilters: VisibleQuickFiltersConfig? = null,
    val sort: LibrarySortSpec = LibrarySortSpec.DEFAULT,
    val entryCardPresentations: List<EntryCardPresentation> = emptyList(),
    val entryHierarchyDisplayMode: EntryHierarchyDisplayMode =
        EntryHierarchyDisplayMode.COLLAPSED
)

enum class EntryHierarchyDisplayMode(val key: String) {
    COLLAPSED("collapsed"),
    EXPANDED("expanded"),
    SEPARATE("separate");

    companion object {
        fun fromKey(key: String?): EntryHierarchyDisplayMode =
            entries.firstOrNull { it.key == key } ?: COLLAPSED
    }
}

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
