package com.aozijx.passly.feature.vault.editor.bankcard

import org.junit.Assert.assertEquals
import org.junit.Test

class CardNumberValidatorTest {
    @Test
    fun `empty card number has an explicit empty result`() {
        assertEquals(CardNumberValidationResult.Empty, CardNumberValidator.validate(""))
    }

    @Test
    fun `invalid length and checksum have typed reasons`() {
        assertEquals(
            CardNumberValidationResult.Invalid(CardNumberInvalidReason.InvalidLength),
            CardNumberValidator.validate("123"),
        )
        assertEquals(
            CardNumberValidationResult.Invalid(CardNumberInvalidReason.ChecksumFailed),
            CardNumberValidator.validate("4111111111111112"),
        )
    }

    @Test
    fun `valid card distinguishes matched and unknown networks`() {
        assertEquals(
            CardNumberValidationResult.Valid(CardNetworkMatch.Matched(CardNetwork.VISA)),
            CardNumberValidator.validate("4111111111111111"),
        )
        assertEquals(
            CardNumberValidationResult.Valid(CardNetworkMatch.Unknown),
            CardNumberValidator.validate("30569309025904"),
        )
    }
}
