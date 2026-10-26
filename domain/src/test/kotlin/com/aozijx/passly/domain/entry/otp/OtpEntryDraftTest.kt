package com.aozijx.passly.domain.entry.otp

import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.policy.EntryDraftMaterializer
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import org.junit.Assert.assertEquals
import org.junit.Test

class OtpEntryDraftTest {
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

        assertEquals(EntryType.OTP, draft.type)
        assertEquals(EntryDraftValue.Text("Example: alice"), draft[FieldKey.TITLE])
        assertEquals(EntryDraftValue.Text("secret"), draft[FieldKey.OTP_SECRET])
        assertEquals(EntryDraftValue.Text("SHA256"), draft[FieldKey.OTP_ALGORITHM])
        assertEquals(EntryDraftValue.Number(45), draft[FieldKey.OTP_PERIOD])
        assertEquals(null, draft[FieldKey.OTP_COUNTER])
    }

    @Test
    fun `title preserves colons and materialization preserves complete config`() {
        val config = OtpConfig(
            secret = "Zm9v", encoding = OtpSecretEncoding.BASE64,
            algorithm = OtpHashAlgorithm.SHA512, digits = 8, periodSeconds = 45,
            issuer = " Operations: West ", accountName = " alice:admin ",
        )
        val draft = config.toNewEntryDraft()
        val entry = EntryDraftMaterializer().materialize(
            draft, EntryIdentity(EntryId("otp"), EntryType.OTP, timestamps = EntryTimestamps(123L)),
        )
        assertEquals("Operations: West: alice:admin", entry.title)
        assertEquals(config, entry.secret.otp?.config)
    }

    @Test
    fun `fallback title follows otp type when identity is absent`() {
        val hotp = OtpConfig(type = OtpType.HOTP, secret = "MY", counter = Long.MAX_VALUE, periodSeconds = null)
        val steam = OtpConfig.steamDefaults("MY")
        assertEquals(EntryDraftValue.Text("HOTP"), hotp.toNewEntryDraft()[FieldKey.TITLE])
        assertEquals(EntryDraftValue.Text("Steam"), steam.toNewEntryDraft()[FieldKey.TITLE])
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
