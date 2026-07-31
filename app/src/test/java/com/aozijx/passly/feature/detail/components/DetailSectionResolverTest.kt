package com.aozijx.passly.feature.detail.components

import com.aozijx.passly.domain.entry.model.EntryHeader
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntrySummary
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.domain.entry.model.secret.IdentitySecret
import com.aozijx.passly.domain.entry.model.secret.OtpSecret
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
    fun `passkey entry adds passkey component without login credential section`() {
        val sections = DetailSectionResolver.resolve(
            entry(
                type = EntryType.PASSKEY,
                secret = EntrySecret(passkey = PasskeySecret(privateKeyReference = "key-ref")),
            )
        )

        assertFalse(DetailSectionKey.CREDENTIAL in sections)
        assertTrue(DetailSectionKey.PASSKEY in sections)
    }

    @Test
    fun `otp entry uses otp component without login credential section`() {
        val sections = DetailSectionResolver.resolve(
            entry(
                type = EntryType.OTP,
                secret = EntrySecret(otp = OtpSecret()),
            )
        )

        assertFalse(DetailSectionKey.CREDENTIAL in sections)
        assertTrue(DetailSectionKey.OTP in sections)
    }

    @Test
    fun `note entry uses notes area without login credential section`() {
        val sections = DetailSectionResolver.resolve(
            entry(
                type = EntryType.NOTE,
                secret = EntrySecret(notes = "# Note"),
            )
        )

        assertFalse(DetailSectionKey.CREDENTIAL in sections)
        assertTrue(DetailSectionKey.NOTES in sections)
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
