package com.aozijx.passly.presentation.feature.vault.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailTagNormalizerTest {

    @Test
    fun normalizeSplitsCommaAndNewlineAndRemovesBlanks() {
        val result = DetailTagNormalizer.normalize(
            listOf(" Work, Personal\n\nFinance ", "Travel"),
        )

        assertEquals(
            linkedSetOf("Finance", "Personal", "Travel", "Work"),
            (result as TagNormalizationResult.Valid).tags,
        )
    }

    @Test
    fun normalizeDeduplicatesIgnoringCaseAndPreservesFirstSpelling() {
        val result = DetailTagNormalizer.normalize(
            listOf("Passly", "passly", "PASSLY", "Work"),
        )

        assertEquals(
            linkedSetOf("Passly", "Work"),
            (result as TagNormalizationResult.Valid).tags,
        )
    }

    @Test
    fun normalizeOrdersTagsCaseInsensitively() {
        val result = DetailTagNormalizer.normalize(listOf("zebra", "Alpha", "beta"))

        assertEquals(
            listOf("Alpha", "beta", "zebra"),
            (result as TagNormalizationResult.Valid).tags.toList(),
        )
    }

    @Test
    fun normalizeRejectsTwentyFirstDistinctTag() {
        val result = DetailTagNormalizer.normalize((1..21).map { "tag-$it" })

        assertEquals(TagNormalizationResult.TooMany(maximum = 20), result)
    }

    @Test
    fun normalizeRejectsTagLongerThanThirtyTwoCharacters() {
        val tooLong = "x".repeat(33)

        val result = DetailTagNormalizer.normalize(listOf("valid", tooLong))

        assertEquals(
            TagNormalizationResult.TooLong(value = tooLong, maximum = 32),
            result,
        )
    }

    @Test
    fun suggestionsMatchPrefixIgnoringCaseAndExcludeSelectedTags() {
        val suggestions = DetailTagNormalizer.suggestions(
            existingTags = setOf("Personal", "passkey", "Password", "Work"),
            prefix = "PAS",
            selectedTags = setOf("PASSKEY"),
        )

        assertEquals(listOf("Password"), suggestions)
    }

    @Test
    fun emptyPrefixHasNoSuggestions() {
        val suggestions = DetailTagNormalizer.suggestions(
            existingTags = setOf("Personal", "Work"),
            prefix = "  ",
            selectedTags = emptySet(),
        )

        assertTrue(suggestions.isEmpty())
    }
}
