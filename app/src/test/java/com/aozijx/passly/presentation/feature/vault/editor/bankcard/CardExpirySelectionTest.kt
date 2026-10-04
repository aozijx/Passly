package com.aozijx.passly.presentation.feature.vault.editor.bankcard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardExpirySelectionTest {
    @Test
    fun yearRange_supportsHistoricalCardsThroughTheCurrentYear() {
        val years = cardExpiryYearRange(currentYear = 2026)

        assertEquals(1900, years.first)
        assertEquals(2026, years.last)
        assertTrue(1995 in years)
    }

    @Test
    fun yearRange_doesNotOfferFutureYears() {
        assertTrue(2027 !in cardExpiryYearRange(currentYear = 2026))
    }

    @Test
    fun withCardExpiry_updatesMonthAndYearAtomically() {
        val form = AddBankCardFormState().withCardExpiry(month = 3, year = 1998)

        assertEquals("03", form.cardExpiryMonth)
        assertEquals("1998", form.cardExpiryYear)
        assertEquals(AddBankCardFormState(), form.withoutCardExpiry())
    }

    @Test(expected = IllegalArgumentException::class)
    fun withCardExpiry_rejectsInvalidMonth() {
        AddBankCardFormState().withCardExpiry(month = 13, year = 2026)
    }
}
