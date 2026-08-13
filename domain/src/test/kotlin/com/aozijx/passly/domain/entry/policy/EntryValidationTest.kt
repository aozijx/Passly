package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryValidationTest {
    @Test
    fun `validation returns stable codes rather than presentation text`() {
        val violations = EntryValidation.validate(
            entry(
                type = EntryType.LOGIN,
                profile = EntryProfile(title = "Login"),
                secret = EntrySecret(LoginCredential()),
            )
        )

        assertEquals(
            setOf(
                EntryViolation(FieldKey.USERNAME, EntryViolationCode.REQUIRED),
                EntryViolation(FieldKey.PASSWORD, EntryViolationCode.REQUIRED),
            ),
            violations,
        )
    }

    @Test
    fun `card validation reports independent field violations`() {
        val violations = EntryValidation.validate(
            entry(
                type = EntryType.BANK_CARD,
                profile = EntryProfile(title = "Card"),
                secret = EntrySecret(
                    CardCredential(
                        cardNumber = "123",
                        cardExpiry = "2028-10",
                        cardCvv = "12",
                    )
                ),
            )
        )

        assertTrue(EntryViolation(FieldKey.CARD_NUMBER, EntryViolationCode.INVALID_LENGTH) in violations)
        assertTrue(EntryViolation(FieldKey.CARD_EXPIRATION, EntryViolationCode.INVALID_FORMAT) in violations)
        assertTrue(EntryViolation(FieldKey.CARD_CVV, EntryViolationCode.INVALID_FORMAT) in violations)
    }

    private fun entry(type: EntryType, profile: EntryProfile, secret: EntrySecret) = Entry(
        identity = EntryIdentity(
            id = EntryId("entry"),
            type = type,
            timestamps = EntryTimestamps(createdAtMs = 1),
        ),
        profile = profile,
        secret = secret,
    )
}
