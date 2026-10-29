package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class EntryDraftMaterializerTest {
    private val materializer = EntryDraftMaterializer()

    @Test
    fun unsupportedTextualValuesAreExcludedFromFailureMessages() {
        val marker = "private-input-marker"
        for (key in listOf(FieldKey.OTP_TYPE, FieldKey.OTP_ALGORITHM, FieldKey.OTP_ENCODING, FieldKey.WIFI_SECURITY)) {
            val type = if (key == FieldKey.WIFI_SECURITY) EntryType.WIFI else EntryType.OTP
            val values = if (type == EntryType.WIFI) {
                arrayOf(FieldKey.TITLE to EntryDraftValue.Text("Network"),
                    FieldKey.WIFI_SSID to EntryDraftValue.Text("Network"), key to EntryDraftValue.Text(marker))
            } else {
                arrayOf(FieldKey.TITLE to EntryDraftValue.Text("Token"),
                    FieldKey.OTP_SECRET to EntryDraftValue.Text("MY"), key to EntryDraftValue.Text(marker))
            }
            val failure = assertThrows(IllegalArgumentException::class.java) {
                materializer.materialize(draft(type, *values), identity(type))
            }
            assertFalse(failure.toString().contains(marker))
        }
    }

    @Test
    fun unsupportedCreationTypeRemainsRejectedAfterDraftValidation() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            materializer.materialize(
                draft(EntryType.NOTE, FieldKey.TITLE to EntryDraftValue.Text("Reminder")),
                identity(EntryType.NOTE),
            )
        }

        assertEquals("Entry creation is not supported for NOTE", failure.message)
    }

    @Test
    fun omittedOptionalOtpFieldsKeepTheirDefaults() {
        val entry = materializer.materialize(draft(EntryType.OTP,
            FieldKey.TITLE to EntryDraftValue.Text("Token"),
            FieldKey.OTP_SECRET to EntryDraftValue.Text("MY"),
        ), identity(EntryType.OTP))

        val config = requireNotNull(entry.secret.otp?.config)
        assertEquals(OtpType.TOTP, config.type)
        assertEquals(6, config.digits)
        assertEquals(30, config.periodSeconds)
        assertNull(config.counter)
    }

    @Test
    fun suppliedEmptyNumbersDoNotAcquireDefaults() {
        for ((key, value) in listOf(
            FieldKey.OTP_DIGITS to EntryDraftValue.Number(null),
            FieldKey.OTP_PERIOD to EntryDraftValue.Number(null),
            FieldKey.OTP_COUNTER to EntryDraftValue.LongNumber(null),
        )) {
            val values = mapOf(
                FieldKey.TITLE to EntryDraftValue.Text("Token"),
                FieldKey.OTP_SECRET to EntryDraftValue.Text("MY"),
                FieldKey.OTP_TYPE to EntryDraftValue.Text(if (key == FieldKey.OTP_COUNTER) "HOTP" else "TOTP"),
                key to value,
            )
            assertThrows(IllegalArgumentException::class.java) {
                materializer.materialize(EntryDraft(EntryType.OTP, values), identity(EntryType.OTP))
            }
        }
    }

    @Test
    fun rawDraftCannotSilentlyDiscardWrongTypesOrUnsupportedFields() {
        for ((key, value) in listOf(
            FieldKey.OTP_DIGITS to EntryDraftValue.Text("8"),
            FieldKey.CARD_HOLDER to EntryDraftValue.Text("private name"),
        )) {
            val values = mapOf(
                FieldKey.TITLE to EntryDraftValue.Text("Token"),
                FieldKey.OTP_SECRET to EntryDraftValue.Text("MY"),
                key to value,
            )
            assertThrows(IllegalArgumentException::class.java) {
                materializer.materialize(EntryDraft(EntryType.OTP, values), identity(EntryType.OTP))
            }
        }
    }

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
    fun materializeOtp_rejectsUnsupportedAlgorithmInsteadOfUsingSha1() {
        assertThrows(IllegalArgumentException::class.java) {
            materializer.materialize(
                draft = draft(EntryType.OTP,
                    FieldKey.TITLE to EntryDraftValue.Text("Token"),
                    FieldKey.OTP_SECRET to EntryDraftValue.Text("ABC123"),
                    FieldKey.OTP_ALGORITHM to EntryDraftValue.Text("MD5"),
                ),
                identity = identity(EntryType.OTP),
            )
        }
    }

    @Test
    fun materializeBankCard_preservesCredentialFieldsWithoutCopyingHolderToProfile() {
        val entry = materializer.materialize(
            draft = draft(EntryType.BANK_CARD,
                FieldKey.TITLE to EntryDraftValue.Text("Visa"),
                FieldKey.CARD_TYPE to EntryDraftValue.Text("CREDIT"),
                FieldKey.CARD_HOLDER to EntryDraftValue.Text("Ada"),
                FieldKey.CARD_ISSUING_BANK to EntryDraftValue.Text("Bank"),
                FieldKey.CARD_BANK_BRANCH to EntryDraftValue.Text("Branch"),
                FieldKey.CARD_PRODUCT to EntryDraftValue.Text("Product"),
                FieldKey.CARD_CURRENCY to EntryDraftValue.Text("CNY / USD"),
                FieldKey.CARD_BANK_PHONE to EntryDraftValue.Text("+86 123456"),
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
        assertEquals("", entry.profile.username)
        assertEquals("Bank", card.issuingBank)
        assertEquals("Branch", card.bankBranch)
        assertEquals("Product", card.cardProduct)
        assertEquals("CNY / USD", card.currency)
        assertEquals("+86 123456", card.bankPhone)
        assertEquals("4111 1111", card.cardNumber)
        assertEquals("03/2030", card.cardExpiry)
        assertEquals("1 23", card.cardCvv)
        assertEquals("1 2 3 4", card.paymentPin)
        assertEquals("London", card.billingAddress)
        assertEquals("primary", entry.secret.notes)
    }

    @Test
    fun materializeWifi_preservesCredentialAndMetadata() {
        val entry = materializer.materialize(
            draft = draft(EntryType.WIFI,
                FieldKey.TITLE to EntryDraftValue.Text("Office network"),
                FieldKey.WIFI_SSID to EntryDraftValue.Text("Passly"),
                FieldKey.PASSWORD to EntryDraftValue.Text("abcde"),
                FieldKey.WIFI_SECURITY to EntryDraftValue.Text("WEP"),
                FieldKey.WIFI_HIDDEN to EntryDraftValue.Toggle(true),
                FieldKey.TAGS to EntryDraftValue.TextList(listOf("work", "network")),
                FieldKey.NOTES to EntryDraftValue.Text("legacy device"),
            ),
            identity = identity(EntryType.WIFI),
        )

        val wifi = requireNotNull(entry.secret.wifi)
        assertEquals("Office network", entry.title)
        assertEquals("Passly", wifi.ssid)
        assertEquals("abcde", wifi.password)
        assertEquals(WifiSecurityType.Wep, wifi.securityType)
        assertEquals(true, wifi.isHidden)
        assertEquals(linkedSetOf("work", "network"), entry.tags)
        assertEquals("legacy device", entry.secret.notes)
    }

    @Test
    fun materializeWifi_rejectsUnsupportedSecurity() {
        assertThrows(IllegalArgumentException::class.java) {
            materializer.materialize(
                draft = draft(EntryType.WIFI,
                    FieldKey.TITLE to EntryDraftValue.Text("Office network"),
                    FieldKey.WIFI_SSID to EntryDraftValue.Text("Passly"),
                    FieldKey.WIFI_SECURITY to EntryDraftValue.Text("vendor-private"),
                ),
                identity = identity(EntryType.WIFI),
            )
        }
    }

    private fun draft(type: EntryType, vararg values: Pair<FieldKey, EntryDraftValue>): EntryDraft {
        val definition = EntryTypeDefinitions[type]
        return values.fold(EntryDraft(type)) { draft, (key, value) ->
            draft.withValue(definition, key, value)
        }
    }

    private fun identity(type: EntryType) = EntryIdentity(
        id = EntryId("entry-1"),
        type = type,
        timestamps = EntryTimestamps(123L),
    )
}
