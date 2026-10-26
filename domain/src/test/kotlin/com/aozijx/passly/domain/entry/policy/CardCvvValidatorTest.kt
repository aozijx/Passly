package com.aozijx.passly.domain.entry.policy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardCvvValidatorTest {
    @Test
    fun `optional absence and ascii codes are accepted`() {
        for (value in listOf(null, "", " \t", "000", "1234")) assertTrue(CardCvvValidator.isValid(value))
    }

    @Test
    fun `wrong length unicode digits and grouping are rejected`() {
        for (value in listOf("12", "12345", "１２３", "١٢٣", "1 23", "12-3", " 123 ")) {
            assertFalse(CardCvvValidator.isValid(value))
        }
    }
}
