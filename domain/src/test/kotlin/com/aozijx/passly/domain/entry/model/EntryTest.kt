package com.aozijx.passly.domain.entry.model

import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import org.junit.Assert.assertThrows
import org.junit.Test

class EntryTest {
    @Test
    fun `entry rejects a credential that does not match its type`() {
        assertThrows(IllegalArgumentException::class.java) {
            entry(
                type = EntryType.LOGIN,
                secret = EntrySecret(CardCredential(cardNumber = "4111111111111111")),
            )
        }
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

    @Test
    fun `login accepts exactly one login credential`() {
        entry(
            type = EntryType.LOGIN,
            secret = EntrySecret(LoginCredential(password = "secret")),
        )
    }

    private fun entry(type: EntryType, secret: EntrySecret) = Entry(
        identity = EntryIdentity(
            id = EntryId("entry"),
            type = type,
            timestamps = EntryTimestamps(createdAtMs = 1),
        ),
        profile = EntryProfile(title = "Example"),
        secret = secret,
    )
}
