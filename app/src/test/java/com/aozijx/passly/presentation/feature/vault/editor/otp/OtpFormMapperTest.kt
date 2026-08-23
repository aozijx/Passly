package com.aozijx.passly.presentation.feature.vault.editor.otp

import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.policy.EntryTypeDefinitions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpFormMapperTest {
    @Test
    fun toEntryDraft_mapsTotpWithoutHotpCounter() {
        val draft = OtpFormState(
            title = " Example ",
            issuer = " Example Inc ",
            accountName = " user@example.com ",
            secret = " AB C ",
            period = "60",
            digits = "8",
            algorithm = "sha256",
            encoding = OtpSecretEncoding.BASE64,
            counter = "99",
            tags = " work, otp ",
            notes = " primary token ",
        ).toEntryDraft()

        assertEquals(EntryDraftValue.Text("Example"), draft[FieldKey.TITLE])
        assertEquals(EntryDraftValue.Text("Example Inc"), draft[FieldKey.OTP_ISSUER])
        assertEquals(EntryDraftValue.Text("user@example.com"), draft[FieldKey.OTP_ACCOUNT_NAME])
        assertEquals(EntryDraftValue.Text("AB C"), draft[FieldKey.OTP_SECRET])
        assertEquals(EntryDraftValue.Text(OtpType.TOTP.name), draft[FieldKey.OTP_TYPE])
        assertEquals(EntryDraftValue.Number(60), draft[FieldKey.OTP_PERIOD])
        assertNull(draft[FieldKey.OTP_COUNTER])
        assertEquals(EntryDraftValue.Number(8), draft[FieldKey.OTP_DIGITS])
        assertEquals(EntryDraftValue.Text("SHA256"), draft[FieldKey.OTP_ALGORITHM])
        assertEquals(EntryDraftValue.Text(OtpSecretEncoding.BASE64.name), draft[FieldKey.OTP_ENCODING])
        assertEquals(EntryDraftValue.TextList(listOf("work", "otp")), draft[FieldKey.TAGS])
        assertEquals(EntryDraftValue.Text("primary token"), draft[FieldKey.NOTES])
        assertTrue(draft.missingRequiredFields(EntryTypeDefinitions[EntryType.OTP]).isEmpty())
    }

    @Test
    fun toEntryDraft_preservesHotpCounterAsLong() {
        val draft = OtpFormState(
            title = "HOTP",
            secret = "secret",
            type = OtpType.HOTP,
            counter = Long.MAX_VALUE.toString(),
        ).toEntryDraft()

        assertEquals(EntryDraftValue.LongNumber(Long.MAX_VALUE), draft[FieldKey.OTP_COUNTER])
        assertNull(draft[FieldKey.OTP_PERIOD])
    }

    @Test
    fun toEntryDraft_forcesSteamDefaults() {
        val draft = OtpFormState(
            title = "Steam",
            secret = "secret",
            type = OtpType.STEAM,
            algorithm = "SHA512",
            digits = "8",
        ).toEntryDraft()

        assertEquals(EntryDraftValue.Number(5), draft[FieldKey.OTP_DIGITS])
        assertEquals(EntryDraftValue.Text("SHA1"), draft[FieldKey.OTP_ALGORITHM])
        assertEquals(EntryDraftValue.Number(30), draft[FieldKey.OTP_PERIOD])
        assertNull(draft[FieldKey.OTP_COUNTER])
    }
}
