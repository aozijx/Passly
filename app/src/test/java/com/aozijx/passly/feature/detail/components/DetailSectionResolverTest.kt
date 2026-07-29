package com.aozijx.passly.feature.detail.components

import com.aozijx.passly.domain.entry.model.EntryHeader
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntrySummary
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.domain.entry.model.secret.IdentitySecret
import com.aozijx.passly.domain.entry.model.secret.PasskeySecret
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailSectionResolverTest {

    @Test
    fun `seed phrase uses its high sensitivity section without identity section`() {
        val sections = DetailSectionResolver.resolve(
            entry(
                type = EntryType.SEED_PHRASE,
                secret = EntrySecret(identity = IdentitySecret(seedPhrase = "one two three")),
            )
        )

        assertTrue(DetailSectionKey.SEED_PHRASE in sections)
        assertFalse(DetailSectionKey.IDENTITY in sections)
    }

    @Test
    fun `mixed entry adds passkey component from structured secret capability`() {
        val sections = DetailSectionResolver.resolve(
            entry(
                type = EntryType.LOGIN,
                secret = EntrySecret(passkey = PasskeySecret(privateKeyReference = "key-ref")),
            )
        )

        assertTrue(DetailSectionKey.CREDENTIAL in sections)
        assertTrue(DetailSectionKey.PASSKEY in sections)
    }

    private fun entry(type: EntryType, secret: EntrySecret) = VaultEntry(
        header = EntryHeader(
            id = EntryId("018f9dd6-66c5-7cc0-85b5-39a337956681"),
            entryType = type,
            version = EntryVersion.INITIAL,
            createdAt = 1L,
            updatedAt = 1L,
        ),
        summary = EntrySummary(title = "Example", username = ""),
        secret = secret,
    )
}
