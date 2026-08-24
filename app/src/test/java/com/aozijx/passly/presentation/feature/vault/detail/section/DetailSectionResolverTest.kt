package com.aozijx.passly.presentation.feature.vault.detail.section

import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.credential.IdentityCredential
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.credential.PasskeyCredential
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailSectionResolverTest {

    @Test
    fun `seed phrase uses its high sensitivity section without identity section`() {
        val sections = DetailSectionResolver.resolve(
            entry(
                type = EntryType.SEED_PHRASE,
                secret = EntrySecret(credential = IdentityCredential(seedPhrase = "one two three")),
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
                secret = EntrySecret(credential = PasskeyCredential(privateKeyReference = "key-ref")),
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
                secret = EntrySecret(credential = OtpCredential(OtpConfig(secret = "secret"))),
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

    private fun entry(type: EntryType, secret: EntrySecret) = Entry(
        identity = EntryIdentity(
            id = EntryId("018f9dd6-66c5-7cc0-85b5-39a337956681"),
            type = type,
            version = EntryVersion.INITIAL,
            timestamps = EntryTimestamps(1L),
        ),
        profile = EntryProfile(title = "Example"),
        secret = secret,
    )
}
