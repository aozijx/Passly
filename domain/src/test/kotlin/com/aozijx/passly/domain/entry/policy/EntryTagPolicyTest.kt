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
    fun normalizeEnforcesMaximumTagCountAfterDeduplication() {
        val atLimit = EntryTagPolicy.normalize((1..20).map { "tag-$it" })
        assertEquals(20, (atLimit as EntryTagNormalizationResult.Valid).tags.size)

        val duplicatesDoNotExceedTheLimit =
            EntryTagPolicy.normalize((1..20).map { "tag-$it" } + listOf("TAG-1", " tag-2 "))
        assertEquals(20, (duplicatesDoNotExceedTheLimit as EntryTagNormalizationResult.Valid).tags.size)

        val result = EntryTagPolicy.normalize((1..21).map { "tag-$it" })

        assertEquals(EntryTagNormalizationResult.TooMany(maximum = 20), result)
    }

    @Test
    fun normalizeEnforcesMaximumTagLength() {
        val atLimit = EntryTagPolicy.normalize(listOf("x".repeat(32)))
        assertEquals(setOf("x".repeat(32)), (atLimit as EntryTagNormalizationResult.Valid).tags)

        val tooLong = "x".repeat(33)

        assertEquals(
            EntryTagNormalizationResult.TooLong(value = tooLong, maximum = 32),
            EntryTagPolicy.normalize(listOf("valid", tooLong)),
        )
    }
}
