package com.aozijx.passly.feature.vault.entry

import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryDraftTarget
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.policy.EntryTypeDefinitions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntryDraftMaterializerTest {
    private val materializer = EntryDraftMaterializer()

    @Test
    fun materializeLogin_preservesCredentialAndMetadata() {
        val entry = materializer.materialize(
            draft = draft(EntryType.LOGIN,
                FieldKey.TITLE to EntryDraftValue.Text("Mail"),
                FieldKey.USERNAME to EntryDraftValue.Text("user@example.com"),
                FieldKey.PASSWORD to EntryDraftValue.Text(" secret "),
                FieldKey.PRIMARY_URL to EntryDraftValue.Text("https://example.com"),
                FieldKey.TAGS to EntryDraftValue.TextList(listOf("work", "mail")),
                FieldKey.NOTES to EntryDraftValue.Text("personal"),
            ),
            identity = identity(EntryType.LOGIN),
        )

        assertEquals("Mail", entry.profile.title)
        assertEquals("user@example.com", entry.profile.username)
        assertEquals("https://example.com", entry.profile.associations.primaryUrl)
        assertEquals(linkedSetOf("work", "mail"), entry.profile.tags)
        assertEquals(" secret ", entry.secret.login?.password)
        assertEquals("personal", entry.secret.notes)
    }

    @Test
    fun materializeHotp_preservesLongCounterAndOtpSemantics() {
        val entry = materializer.materialize(
            draft = draft(EntryType.OTP,
                FieldKey.TITLE to EntryDraftValue.Text("Token"),
                FieldKey.OTP_SECRET to EntryDraftValue.Text("ABC 123"),
                FieldKey.OTP_TYPE to EntryDraftValue.Text("HOTP"),
                FieldKey.OTP_COUNTER to EntryDraftValue.LongNumber(Long.MAX_VALUE),
                FieldKey.OTP_DIGITS to EntryDraftValue.Number(8),
                FieldKey.OTP_ALGORITHM to EntryDraftValue.Text("SHA512"),
                FieldKey.OTP_ENCODING to EntryDraftValue.Text("BASE64"),
                FieldKey.OTP_ISSUER to EntryDraftValue.Text("Example"),
                FieldKey.OTP_ACCOUNT_NAME to EntryDraftValue.Text("ada@example.com"),
            ),
            identity = identity(EntryType.OTP),
        )

        val config = requireNotNull(entry.secret.otp?.config)
        assertEquals(OtpType.HOTP, config.type)
        assertEquals("ABC 123", config.secret)
        assertEquals(Long.MAX_VALUE, config.counter)
        assertNull(config.periodSeconds)
        assertEquals(8, config.digits)
        assertEquals(OtpHashAlgorithm.SHA512, config.algorithm)
        assertEquals(OtpSecretEncoding.BASE64, config.encoding)
        assertEquals("Example", config.issuer)
        assertEquals("ada@example.com", config.accountName)
    }

    @Test
    fun materializeBankCard_preservesAllCardFields() {
        val entry = materializer.materialize(
            draft = draft(EntryType.BANK_CARD,
                FieldKey.TITLE to EntryDraftValue.Text("Visa"),
                FieldKey.CARD_TYPE to EntryDraftValue.Text("CREDIT"),
                FieldKey.CARD_HOLDER to EntryDraftValue.Text("Ada"),
                FieldKey.CARD_NUMBER to EntryDraftValue.Text("4111 1111"),
                FieldKey.CARD_EXPIRATION to EntryDraftValue.Text("03/2030"),
                FieldKey.CARD_CVV to EntryDraftValue.Text("1 23"),
                FieldKey.PAYMENT_PIN to EntryDraftValue.Text("1 2 3 4"),
                FieldKey.BILLING_ADDRESS to EntryDraftValue.Text("London"),
                FieldKey.NOTES to EntryDraftValue.Text("primary"),
            ),
            identity = identity(EntryType.BANK_CARD),
        )

        val card = requireNotNull(entry.secret.card)
        assertEquals("CREDIT", card.cardType)
        assertEquals("Ada", card.cardHolder)
        assertEquals("4111 1111", card.cardNumber)
        assertEquals("03/2030", card.cardExpiry)
        assertEquals("1 23", card.cardCvv)
        assertEquals("1 2 3 4", card.paymentPin)
        assertEquals("London", card.billingAddress)
        assertEquals("primary", entry.secret.notes)
    }

    private fun draft(type: EntryType, vararg values: Pair<FieldKey, EntryDraftValue>): EntryDraft {
        val definition = EntryTypeDefinitions[type]
        return values.fold(EntryDraft(EntryDraftTarget.New(type))) { draft, (key, value) ->
            draft.withValue(definition, key, value)
        }
    }

    private fun identity(type: EntryType) = EntryIdentity(
        id = EntryId("entry-1"),
        type = type,
        timestamps = EntryTimestamps(123L),
    )
}
