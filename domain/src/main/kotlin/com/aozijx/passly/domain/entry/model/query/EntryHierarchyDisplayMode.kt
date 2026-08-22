package com.aozijx.passly.domain.entry.model.query

/** Global hierarchy projection applied before Paging slices the result set. */
enum class EntryHierarchyDisplayMode(val key: String) {
    COLLAPSED("collapsed"),
    EXPANDED("expanded"),
    SEPARATE("separate");

    companion object {
        fun fromKey(key: String?): EntryHierarchyDisplayMode =
            entries.firstOrNull { it.key == key } ?: COLLAPSED
    }
}
