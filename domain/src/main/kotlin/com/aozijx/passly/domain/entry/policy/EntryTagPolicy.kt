package com.aozijx.passly.domain.entry.policy

import java.util.Locale

sealed interface EntryTagNormalizationResult {
    data class Valid(
        val tags: Set<String>,
    ) : EntryTagNormalizationResult

    data class TooMany(
        val maximum: Int,
    ) : EntryTagNormalizationResult

    data class TooLong(
        val value: String,
        val maximum: Int,
    ) : EntryTagNormalizationResult
}

/** Owns tag normalization and size constraints applied before Entry tag updates. */
object EntryTagPolicy {
    const val MAX_TAG_COUNT = 20
    const val MAX_TAG_LENGTH = 32

    fun normalize(rawTags: Iterable<String>): EntryTagNormalizationResult {
        val distinct = linkedMapOf<String, String>()
        rawTags
            .flatMap { raw -> raw.split(TAG_SEPARATOR) }
            .map(String::trim)
            .filter(String::isNotEmpty)
            .forEach { tag -> distinct.putIfAbsent(tag.normalizedKey(), tag) }

        distinct.values.firstOrNull { it.length > MAX_TAG_LENGTH }?.let { tag ->
            return EntryTagNormalizationResult.TooLong(tag, MAX_TAG_LENGTH)
        }
        if (distinct.size > MAX_TAG_COUNT) {
            return EntryTagNormalizationResult.TooMany(MAX_TAG_COUNT)
        }

        val sorted =
            distinct.values
                .sortedWith(String.CASE_INSENSITIVE_ORDER.thenBy { it })
                .toCollection(linkedSetOf())
        return EntryTagNormalizationResult.Valid(sorted)
    }

    private fun String.normalizedKey(): String = lowercase(Locale.ROOT)

    private val TAG_SEPARATOR = Regex("[,\\r\\n]+")
}
