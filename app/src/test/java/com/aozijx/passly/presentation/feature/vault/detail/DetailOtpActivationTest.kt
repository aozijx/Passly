package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailOtpActivationTest {
    @Test
    fun `low sensitivity otp bundle still transfers refresh ownership to detail`() {
        val entry = Entry(
            identity = EntryIdentity(
                id = EntryId("otp-entry"),
                type = EntryType.OTP,
                version = EntryVersion.INITIAL,
                timestamps = EntryTimestamps(1L),
            ),
            profile = EntryProfile(title = "OTP"),
            secret = EntrySecret(
                credential = OtpCredential(OtpConfig(secret = null)),
            ),
        )

        assertTrue(entry.shouldAutoActivateOtp())
    }
}
