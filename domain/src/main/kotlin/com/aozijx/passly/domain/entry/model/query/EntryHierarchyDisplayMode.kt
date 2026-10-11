package com.aozijx.passly.domain.entry.model.query

/** Global hierarchy projection applied before Paging slices the result set. */
enum class EntryHierarchyDisplayMode(val key: String) {
    COLLAPSED("collapsed"),
    EXPANDED("expanded"),
    SEPARATE("separate");

    companion object {
        fun parseStoredKey(key: String?): EntryHierarchyDisplayModeParseResult {
            if (key.isNullOrBlank()) return EntryHierarchyDisplayModeParseResult.Missing
            val value = entries.firstOrNull { it.key == key }
            return if (value == null) {
                EntryHierarchyDisplayModeParseResult.Unsupported(key)
            } else {
                EntryHierarchyDisplayModeParseResult.Supported(value)
            }
        }
    }
}

sealed interface EntryHierarchyDisplayModeParseResult {
    data class Supported(
        val value: EntryHierarchyDisplayMode,
    ) : EntryHierarchyDisplayModeParseResult

    data object Missing : EntryHierarchyDisplayModeParseResult

    data class Unsupported(
        val storedValue: String,
    ) : EntryHierarchyDisplayModeParseResult
}
