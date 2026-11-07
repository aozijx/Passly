package com.aozijx.passly.domain.entry.policy

import org.junit.Assert.assertEquals
import org.junit.Test

class EntryTagPolicyTest {
    @Test
    fun normalizeSplitsCommaAndNewlineAndRemovesBlanks() {
        val result =
            EntryTagPolicy.normalize(
                listOf(" Work, Personal\n\nFinance ", "Travel"),
            )

        assertEquals(
            linkedSetOf("Finance", "Personal", "Travel", "Work"),
            (result as EntryTagNormalizationResult.Valid).tags,
        )
    }

    @Test
    fun normalizeDeduplicatesIgnoringCaseAndPreservesFirstSpelling() {
        val result = EntryTagPolicy.normalize(listOf("Passly", "passly", "PASSLY", "Work"))

        assertEquals(
            linkedSetOf("Passly", "Work"),
            (result as EntryTagNormalizationResult.Valid).tags,
        )
    }

    @Test
    fun normalizeOrdersTagsCaseInsensitively() {
        val result = EntryTagPolicy.normalize(listOf("zebra", "Alpha", "beta"))

        assertEquals(
            listOf("Alpha", "beta", "zebra"),
            (result as EntryTagNormalizationResult.Valid).tags.toList(),
        )
    }

    @Test
    fun normalizeRejectsMoreThanMaximumTagCount() {
        val result = EntryTagPolicy.normalize((1..21).map { "tag-$it" })

        assertEquals(EntryTagNormalizationResult.TooMany(maximum = 20), result)
    }

    @Test
    fun normalizeRejectsOverlongTag() {
        val tooLong = "x".repeat(33)

        assertEquals(
            EntryTagNormalizationResult.TooLong(value = tooLong, maximum = 32),
            EntryTagPolicy.normalize(listOf("valid", tooLong)),
        )
    }
}
