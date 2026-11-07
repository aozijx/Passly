package com.aozijx.passly.feature.vault.presentation.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailTagSuggestionsTest {
    @Test
    fun suggestionsMatchPrefixIgnoringCaseAndExcludeSelectedTags() {
        val suggestions =
            DetailTagSuggestions.forInput(
                existingTags = setOf("Personal", "passkey", "Password", "Work"),
                prefix = "PAS",
                selectedTags = setOf("PASSKEY"),
            )

        assertEquals(listOf("Password"), suggestions)
    }

    @Test
    fun emptyPrefixHasNoSuggestions() {
        val suggestions =
            DetailTagSuggestions.forInput(
                existingTags = setOf("Personal", "Work"),
                prefix = "  ",
                selectedTags = emptySet(),
            )

        assertTrue(suggestions.isEmpty())
    }
}
