package com.aozijx.passly.domain.entry.model

import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import org.junit.Assert.assertThrows
import org.junit.Test

class EntryTest {
    @Test
    fun `entry requires credentials to match its type`() {
        assertThrows(IllegalArgumentException::class.java) {
            entry(
                type = EntryType.LOGIN,
                secret = EntrySecret(CardCredential(cardNumber = "4111111111111111")),
            )
        }
        entry(
            type = EntryType.LOGIN,
            secret = EntrySecret(LoginCredential(password = "secret")),
        )
    }

    @Test
    fun `account rejects every secret value`() {
        assertThrows(IllegalArgumentException::class.java) {
            entry(
                type = EntryType.ACCOUNT,
                secret = EntrySecret(notes = "must not live on a grouping entry"),
            )
        }
    }

    private fun entry(
        type: EntryType,
        secret: EntrySecret,
    ) = Entry(
        identity =
            EntryIdentity(
                id = EntryId("entry"),
                type = type,
                timestamps = EntryTimestamps(createdAtMs = 1),
            ),
        profile = EntryProfile(title = "Example"),
        secret = secret,
    )
}
