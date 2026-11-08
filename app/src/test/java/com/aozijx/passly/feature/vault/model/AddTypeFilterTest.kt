package com.aozijx.passly.feature.vault.model

import com.aozijx.passly.domain.entry.model.EntryType
import org.junit.Assert.assertEquals
import org.junit.Test

class AddTypeFilterTest {
    @Test
    fun everyAddTypeMapsToItsExactEntryTypes() {
        val expected =
            mapOf(
                AddType.PASSWORD to
                    setOf(
                        EntryType.LOGIN,
                        EntryType.DATABASE_CREDENTIAL,
                        EntryType.SERVER_CREDENTIAL,
                        EntryType.API_KEY,
                        EntryType.CRYPTO_WALLET,
                    ),
                AddType.TOTP to setOf(EntryType.OTP),
                AddType.BANK_CARD to setOf(EntryType.BANK_CARD),
                AddType.WIFI to setOf(EntryType.WIFI),
                AddType.SSH_KEY to setOf(EntryType.SSH_KEY),
                AddType.ID_CARD to setOf(EntryType.ID_CARD, EntryType.PASSPORT, EntryType.DRIVER_LICENSE),
                AddType.SEED_PHRASE to setOf(EntryType.SEED_PHRASE),
                AddType.PASSKEY to setOf(EntryType.PASSKEY),
                AddType.RECOVERY_CODE to setOf(EntryType.RECOVERY_CODE),
            )

        assertEquals(AddType.entries.toSet(), expected.keys)
        expected.forEach { (addType, entryTypes) ->
            assertEquals(addType.name, entryTypes, addType.entryTypes)
        }
    }
}
