package com.aozijx.passly.feature.vault.presentation.detail

import java.util.Locale

/** Presentation-only suggestions shown while editing the selected Entry's tags. */
internal object DetailTagSuggestions {
    fun forInput(
        existingTags: Set<String>,
        prefix: String,
        selectedTags: Set<String>,
    ): List<String> {
        val query = prefix.trim()
        if (query.isEmpty()) return emptyList()
        val selectedKeys = selectedTags.mapTo(hashSetOf()) { it.normalizedKey() }
        return existingTags
            .asSequence()
            .filter { it.startsWith(query, ignoreCase = true) }
            .filterNot { it.normalizedKey() in selectedKeys }
            .distinctBy { it.normalizedKey() }
            .sortedWith(String.CASE_INSENSITIVE_ORDER.thenBy { it })
            .toList()
    }

    private fun String.normalizedKey(): String = lowercase(Locale.ROOT)
}
