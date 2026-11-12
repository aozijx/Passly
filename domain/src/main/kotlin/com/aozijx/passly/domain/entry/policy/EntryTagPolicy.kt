package com.aozijx.passly.domain.entry.policy

import java.util.Locale

sealed interface EntryTagValidationError {
    data class TooMany(
        val maximum: Int,
    ) : EntryTagValidationError

    data class TooLong(
        val maximum: Int,
    ) : EntryTagValidationError
}

sealed interface EntryTagNormalizationResult {
    data class Valid(
        val tags: Set<String>,
    ) : EntryTagNormalizationResult

    data class Invalid(
        val error: EntryTagValidationError,
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

        distinct.values.firstOrNull { it.length > MAX_TAG_LENGTH }?.let {
            return EntryTagNormalizationResult.Invalid(
                EntryTagValidationError.TooLong(MAX_TAG_LENGTH),
            )
        }
        if (distinct.size > MAX_TAG_COUNT) {
            return EntryTagNormalizationResult.Invalid(
                EntryTagValidationError.TooMany(MAX_TAG_COUNT),
            )
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
