package com.aozijx.passly.feature.vault.entry

import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import org.junit.Assert.assertEquals
import org.junit.Test

class OtpEntryDraftFactoryTest {
    @Test
    fun scannedTotpPreservesConfigAndBuildsDisplayTitle() {
        val draft = OtpConfig(
            type = OtpType.TOTP,
            secret = "secret",
            algorithm = OtpHashAlgorithm.SHA256,
            digits = 8,
            periodSeconds = 45,
            encoding = OtpSecretEncoding.BASE64,
            issuer = "Example",
            accountName = "alice",
        ).toNewEntryDraft()

        assertEquals(EntryType.OTP, draft.target.type)
        assertEquals(EntryDraftValue.Text("Example: alice"), draft[FieldKey.TITLE])
        assertEquals(EntryDraftValue.Text("secret"), draft[FieldKey.OTP_SECRET])
        assertEquals(EntryDraftValue.Text("SHA256"), draft[FieldKey.OTP_ALGORITHM])
        assertEquals(EntryDraftValue.Number(45), draft[FieldKey.OTP_PERIOD])
        assertEquals(null, draft[FieldKey.OTP_COUNTER])
    }

    @Test
    fun scannedHotpPreservesCounterWithoutPeriod() {
        val draft = OtpConfig(
            type = OtpType.HOTP,
            secret = "secret",
            counter = 42L,
            periodSeconds = null,
            accountName = "alice",
        ).toNewEntryDraft()

        assertEquals(EntryDraftValue.Text("alice"), draft[FieldKey.TITLE])
        assertEquals(EntryDraftValue.LongNumber(42L), draft[FieldKey.OTP_COUNTER])
        assertEquals(null, draft[FieldKey.OTP_PERIOD])
    }
}
