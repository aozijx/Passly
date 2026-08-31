package com.aozijx.passly.presentation.feature.vault.detail

import java.util.Locale

internal sealed interface TagNormalizationResult {
    data class Valid(val tags: Set<String>) : TagNormalizationResult
    data class TooMany(val maximum: Int) : TagNormalizationResult
    data class TooLong(val value: String, val maximum: Int) : TagNormalizationResult
}

internal object DetailTagNormalizer {
    const val MAX_TAG_COUNT = 20
    const val MAX_TAG_LENGTH = 32

    fun normalize(rawTags: Iterable<String>): TagNormalizationResult {
        val distinct = linkedMapOf<String, String>()
        rawTags
            .flatMap { raw -> raw.split(TAG_SEPARATOR) }
            .map(String::trim)
            .filter(String::isNotEmpty)
            .forEach { tag -> distinct.putIfAbsent(tag.normalizedKey(), tag) }

        distinct.values.firstOrNull { it.length > MAX_TAG_LENGTH }?.let { tag ->
            return TagNormalizationResult.TooLong(tag, MAX_TAG_LENGTH)
        }
        if (distinct.size > MAX_TAG_COUNT) {
            return TagNormalizationResult.TooMany(MAX_TAG_COUNT)
        }

        val sorted = distinct.values
            .sortedWith(String.CASE_INSENSITIVE_ORDER.thenBy { it })
            .toCollection(linkedSetOf())
        return TagNormalizationResult.Valid(sorted)
    }

    fun suggestions(
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

    private val TAG_SEPARATOR = Regex("[,\\r\\n]+")
}
