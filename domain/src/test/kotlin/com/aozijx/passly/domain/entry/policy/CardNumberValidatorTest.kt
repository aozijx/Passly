package com.aozijx.passly.domain.entry.policy

import org.junit.Assert.assertEquals
import org.junit.Test

class CardNumberValidatorTest {
    @Test
    fun `mastercard two series includes prefixes ending in zero and both boundaries`() {
        listOf(
            "2221000000000009",
            "2230000000000008",
            "2290000000000005",
            "2720990000000007",
        ).forEach { number ->
            assertEquals(
                CardNumberValidationResult.Valid(CardNetworkMatch.Matched(CardNetwork.MASTERCARD)),
                CardNumberValidator.validate(number),
            )
        }
    }

    @Test
    fun `mastercard two series excludes adjacent ranges`() {
        listOf("2220990000000002", "2721000000000004").forEach { number ->
            assertEquals(
                CardNumberValidationResult.Valid(CardNetworkMatch.Unknown),
                CardNumberValidator.validate(number),
            )
        }
    }

    @Test
    fun `formatted card numbers are normalized before validation`() {
        assertEquals(
            CardNumberValidationResult.Valid(CardNetworkMatch.Matched(CardNetwork.MASTERCARD)),
            CardNumberValidator.validate("2230 0000-0000 0008"),
        )
    }

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
