package com.aozijx.passly.feature.vault.model

import com.aozijx.passly.domain.entry.model.EntryType
import org.junit.Assert.assertEquals
import org.junit.Test

class AddTypeFilterTest {
    @Test
    fun passwordFilterIncludesEveryLoginCredentialType() {
        assertEquals(
            setOf(
                EntryType.LOGIN,
                EntryType.DATABASE_CREDENTIAL,
                EntryType.SERVER_CREDENTIAL,
                EntryType.API_KEY,
                EntryType.CRYPTO_WALLET,
            ),
            AddType.PASSWORD.entryTypes,
        )
    }

    @Test
    fun identityFilterIncludesEveryIdentityDocumentType() {
        assertEquals(
            setOf(EntryType.ID_CARD, EntryType.PASSPORT, EntryType.DRIVER_LICENSE),
            AddType.ID_CARD.entryTypes,
        )
    }
}
