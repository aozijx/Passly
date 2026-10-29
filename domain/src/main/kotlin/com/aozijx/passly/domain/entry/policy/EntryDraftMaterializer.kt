package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityTypeParseResult
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.otp.OtpConfigValidation

/** Pure conversion with input checks; callers own complete credential validation, identity creation, access and persistence. */
class EntryDraftMaterializer {
    fun materialize(draft: EntryDraft, identity: EntryIdentity): Entry {
        require(identity.type == draft.type) { "Draft and identity types must match" }
        val definition = EntryTypeDefinitions[identity.type]
        val invalidFields = draft.invalidProvidedFields(definition) + draft.missingRequiredFields(definition)
        if (invalidFields.isNotEmpty()) throw InvalidEntryDraft(invalidFields)
        return when (identity.type) {
            EntryType.LOGIN -> materializeLogin(draft, identity)
            EntryType.OTP -> materializeOtp(draft, identity)
            EntryType.BANK_CARD -> materializeBankCard(draft, identity)
            EntryType.WIFI -> materializeWifi(draft, identity)
            EntryType.ACCOUNT,
            EntryType.NOTE,
            EntryType.ID_CARD,
            EntryType.PASSPORT,
            EntryType.DRIVER_LICENSE,
            EntryType.SSH_KEY,
            EntryType.PASSKEY,
            EntryType.DATABASE_CREDENTIAL,
            EntryType.SERVER_CREDENTIAL,
            EntryType.API_KEY,
            EntryType.CRYPTO_WALLET,
            EntryType.SEED_PHRASE,
            EntryType.RECOVERY_CODE ->
                throw IllegalArgumentException("Entry creation is not supported for ${identity.type}")
        }
    }

    private fun materializeLogin(draft: EntryDraft, identity: EntryIdentity) = Entry(
        identity = identity,
        profile = draft.profile(
            username = draft.text(FieldKey.USERNAME).orEmpty(),
            associations = EntryAssociations(
                primaryUrl = draft.text(FieldKey.PRIMARY_URL),
                domains = draft.textList(FieldKey.DOMAINS).toCollection(linkedSetOf()),
                applicationIds = draft.textList(FieldKey.APPLICATION_IDS).toCollection(linkedSetOf()),
            ),
        ),
        secret = EntrySecret(
            credential = LoginCredential(
                email = draft.text(FieldKey.EMAIL),
                password = draft.text(FieldKey.PASSWORD),
            ),
            notes = draft.text(FieldKey.NOTES),
        ),
    )

    private fun materializeOtp(draft: EntryDraft, identity: EntryIdentity): Entry {
        val type = draft.otpType()
        val algorithm = draft.otpAlgorithm()
        val encoding = draft.otpEncoding()
        val digits = draft.number(FieldKey.OTP_DIGITS) ?: if (type == OtpType.STEAM) 5 else 6
        val period = if (type == OtpType.HOTP) null else draft.number(FieldKey.OTP_PERIOD) ?: 30
        val counter = if (type == OtpType.HOTP) draft.longNumber(FieldKey.OTP_COUNTER) ?: 0L else null
        val invalidParameters = OtpConfigValidation.validateParameters(type, algorithm, digits, period, counter)
        if (invalidParameters.isNotEmpty()) throw InvalidEntryDraft(invalidParameters)
        val config = OtpConfig(
            type = type,
            secret = draft.text(FieldKey.OTP_SECRET),
            algorithm = algorithm,
            digits = digits,
            periodSeconds = period,
            counter = counter,
            encoding = encoding,
            issuer = draft.text(FieldKey.OTP_ISSUER),
            accountName = draft.text(FieldKey.OTP_ACCOUNT_NAME),
        )
        return Entry(
            identity = identity,
            profile = draft.profile(),
            secret = EntrySecret(
                credential = OtpCredential(config),
                notes = draft.text(FieldKey.NOTES),
            ),
        )
    }

    private fun materializeBankCard(draft: EntryDraft, identity: EntryIdentity) = Entry(
        identity = identity,
        profile = draft.profile(),
        secret = EntrySecret(
            credential = CardCredential(
                cardType = draft.text(FieldKey.CARD_TYPE),
                cardNumber = draft.text(FieldKey.CARD_NUMBER),
                cardExpiry = draft.text(FieldKey.CARD_EXPIRATION),
                cardCvv = draft.text(FieldKey.CARD_CVV),
                cardHolder = draft.text(FieldKey.CARD_HOLDER),
                paymentPin = draft.text(FieldKey.PAYMENT_PIN),
                paymentPlatform = draft.text(FieldKey.PAYMENT_PLATFORM),
                billingAddress = draft.text(FieldKey.BILLING_ADDRESS),
                issuingBank = draft.text(FieldKey.CARD_ISSUING_BANK),
                bankBranch = draft.text(FieldKey.CARD_BANK_BRANCH),
                cardProduct = draft.text(FieldKey.CARD_PRODUCT),
                currency = draft.text(FieldKey.CARD_CURRENCY),
                bankPhone = draft.text(FieldKey.CARD_BANK_PHONE),
            ),
            notes = draft.text(FieldKey.NOTES),
        ),
    )

    private fun materializeWifi(draft: EntryDraft, identity: EntryIdentity) = Entry(
        identity = identity,
        profile = draft.profile(),
        secret = EntrySecret(
            credential = WifiCredential(
                ssid = requireNotNull(draft.text(FieldKey.WIFI_SSID)),
                password = draft.text(FieldKey.PASSWORD),
                securityType = draft.text(FieldKey.WIFI_SECURITY).requireWifiSecurityType(),
                isHidden = draft.toggle(FieldKey.WIFI_HIDDEN),
            ),
            notes = draft.text(FieldKey.NOTES),
        ),
    )

    private fun EntryDraft.profile(
        username: String = "",
        associations: EntryAssociations = EntryAssociations(),
    ) = EntryProfile(
        title = requireNotNull(text(FieldKey.TITLE)),
        username = username,
        associations = associations,
        tags = textList(FieldKey.TAGS).toCollection(linkedSetOf()),
    )

    private fun EntryDraft.text(key: FieldKey): String? =
        (this[key] as? EntryDraftValue.Text)?.value

    private fun EntryDraft.textList(key: FieldKey): List<String> =
        (this[key] as? EntryDraftValue.TextList)?.values.orEmpty()

    private fun EntryDraft.number(key: FieldKey): Int? =
        (this[key] as? EntryDraftValue.Number)?.value

    private fun EntryDraft.longNumber(key: FieldKey): Long? =
        (this[key] as? EntryDraftValue.LongNumber)?.value

    private fun EntryDraft.toggle(key: FieldKey): Boolean =
        (this[key] as? EntryDraftValue.Toggle)?.enabled ?: false

    private fun EntryDraft.otpType(): OtpType = when (text(FieldKey.OTP_TYPE)) {
        null -> OtpType.TOTP
        "TOTP" -> OtpType.TOTP
        "HOTP" -> OtpType.HOTP
        "STEAM" -> OtpType.STEAM
        else -> throw InvalidEntryDraft(setOf(FieldKey.OTP_TYPE))
    }

    private fun EntryDraft.otpAlgorithm(): OtpHashAlgorithm =
        when (text(FieldKey.OTP_ALGORITHM)) {
            null -> OtpHashAlgorithm.SHA1
            "SHA1" -> OtpHashAlgorithm.SHA1
            "SHA256" -> OtpHashAlgorithm.SHA256
            "SHA512" -> OtpHashAlgorithm.SHA512
            else -> throw InvalidEntryDraft(setOf(FieldKey.OTP_ALGORITHM))
        }

    private fun EntryDraft.otpEncoding(): OtpSecretEncoding =
        when (text(FieldKey.OTP_ENCODING)) {
            null -> OtpSecretEncoding.BASE32
            "BASE32" -> OtpSecretEncoding.BASE32
            "BASE64" -> OtpSecretEncoding.BASE64
            else -> throw InvalidEntryDraft(setOf(FieldKey.OTP_ENCODING))
        }

    private fun String?.requireWifiSecurityType(): WifiSecurityType =
        when (val result = WifiSecurityType.parseStoredValue(this)) {
            is WifiSecurityTypeParseResult.Supported -> result.value
            WifiSecurityTypeParseResult.Missing,
            is WifiSecurityTypeParseResult.Unsupported ->
                throw InvalidEntryDraft(setOf(FieldKey.WIFI_SECURITY))
        }
}

/** Expected input failure. Carries field identifiers only, never draft values. */
class InvalidEntryDraft(fields: Set<FieldKey>) : IllegalArgumentException("Invalid entry draft") {
    val fields: Set<FieldKey> = fields.toSet()

    init {
        require(fields.isNotEmpty()) { "Invalid draft must identify a field" }
    }
}
