package com.aozijx.passly.feature.vault.presentation.editor.bankcard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardExpirySelectionTest {
    @Test
    fun yearRangeIncludesCurrentYearAndTwentyFutureYearsOnly() {
        val years = cardExpiryYearRange(currentYear = 2026)

        assertEquals(2026, years.first)
        assertEquals(2046, years.last)
        assertTrue(2038 in years)
        assertTrue(2025 !in years)
    }
}
