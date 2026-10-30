package com.aozijx.passly.feature.vault.presentation.editor.otp

import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.policy.EntryTypeDefinitions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class OtpFormMapperTest {
    @Test
    fun fromEntryRestoresCompleteOtpConfigurationForEditing() {
        val config = com.aozijx.passly.domain.entry.model.otp.OtpConfig(
            type = OtpType.HOTP,
            secret = "JBSWY3DPEHPK3PXP",
            algorithm = OtpHashAlgorithm.SHA512,
            digits = 8,
            periodSeconds = null,
            counter = 99,
            encoding = OtpSecretEncoding.BASE32,
            issuer = "Example",
            accountName = "ada",
        )
        val entry = Entry(
            EntryIdentity(EntryId("otp"), EntryType.OTP, timestamps = EntryTimestamps(1)),
            EntryProfile("Token", tags = setOf("work")),
            EntrySecret(credential = OtpCredential(config), notes = "note"),
        )

        val draft = OtpFormState().fromEntry(entry).toEntryDraft()

        assertEquals(EntryDraftValue.Text("JBSWY3DPEHPK3PXP"), draft[FieldKey.OTP_SECRET])
        assertEquals(EntryDraftValue.Text(OtpType.HOTP.name), draft[FieldKey.OTP_TYPE])
        assertEquals(EntryDraftValue.LongNumber(99), draft[FieldKey.OTP_COUNTER])
        assertEquals(EntryDraftValue.Text("Example"), draft[FieldKey.OTP_ISSUER])
    }

    @Test
    fun toEntryDraft_mapsTotpWithoutHotpCounter() {
        val draft = OtpFormState(
            title = " Example ",
            issuer = " Example Inc ",
            accountName = " user@example.com ",
            secret = " Zm9v ",
            period = "60",
            digits = "8",
            algorithm = OtpHashAlgorithm.SHA256,
            encoding = OtpSecretEncoding.BASE64,
            counter = "99",
            tags = " work, otp ",
            notes = " primary token ",
        ).toEntryDraft()

        assertEquals(EntryDraftValue.Text("Example"), draft[FieldKey.TITLE])
        assertEquals(EntryDraftValue.Text("Example Inc"), draft[FieldKey.OTP_ISSUER])
        assertEquals(EntryDraftValue.Text("user@example.com"), draft[FieldKey.OTP_ACCOUNT_NAME])
        assertEquals(EntryDraftValue.Text("Zm9v"), draft[FieldKey.OTP_SECRET])
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
            secret = "JBSWY3DPEHPK3PXP",
            type = OtpType.HOTP,
            counter = Long.MAX_VALUE.toString(),
        ).toEntryDraft()

        assertEquals(EntryDraftValue.LongNumber(Long.MAX_VALUE), draft[FieldKey.OTP_COUNTER])
        assertNull(draft[FieldKey.OTP_PERIOD])
    }

    @Test
    fun toEntryDraft_mapsValidatedSteamParameters() {
        val draft = OtpFormState(
            title = "Steam",
            secret = "JBSWY3DPEHPK3PXP",
            type = OtpType.STEAM,
            algorithm = OtpHashAlgorithm.SHA1,
            digits = "5",
        ).toEntryDraft()

        assertEquals(EntryDraftValue.Number(5), draft[FieldKey.OTP_DIGITS])
        assertEquals(EntryDraftValue.Text("SHA1"), draft[FieldKey.OTP_ALGORITHM])
        assertEquals(EntryDraftValue.Number(30), draft[FieldKey.OTP_PERIOD])
        assertNull(draft[FieldKey.OTP_COUNTER])
    }

    @Test
    fun toEntryDraft_rejectsInvalidNumbersInsteadOfDefaulting() {
        val valid = OtpFormState(title = "OTP", secret = "JBSWY3DPEHPK3PXP")
        for (form in listOf(valid.copy(period = "bad"), valid.copy(digits = "bad"),
            valid.copy(type = OtpType.HOTP, counter = "bad"))) {
            assertThrows(IllegalArgumentException::class.java) { form.toEntryDraft() }
        }
    }
}
