package com.aozijx.passly.presentation.feature.vault.editor.bankcard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardExpirySelectionTest {
    @Test
    fun yearRange_startsNowAndCoversTwentyFutureYears() {
        val years = cardExpiryYearRange(currentYear = 2026)

        assertEquals(2026, years.first)
        assertEquals(2046, years.last)
        assertTrue(2038 in years)
    }

    @Test
    fun yearRange_excludesExpiredYears() {
        val years = cardExpiryYearRange(currentYear = 2026)

        assertTrue(2025 !in years)
    }

    @Test
    fun selectedExpiryKeepsMonthAndYearAtomic() {
        val expiry = CardExpirySelection.Selected(month = 3, year = 1998)

        assertEquals("03/1998", expiry.storageValue)
    }

    @Test(expected = IllegalArgumentException::class)
    fun selectedExpiryRejectsInvalidMonth() {
        CardExpirySelection.Selected(month = 13, year = 2026)
    }
}
