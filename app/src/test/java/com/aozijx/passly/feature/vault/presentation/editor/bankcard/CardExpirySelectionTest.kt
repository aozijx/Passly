package com.aozijx.passly.feature.vault.presentation.editor.bankcard

import org.junit.Assert.assertEquals
import org.junit.Test

class CardExpirySelectionTest {
    @Test
    fun yearRangeIncludesCurrentYearAndTwentyFutureYearsOnly() {
        val years = cardExpiryYearRange(currentYear = 2026)

        assertEquals(2026..2046, years)
    }
}
