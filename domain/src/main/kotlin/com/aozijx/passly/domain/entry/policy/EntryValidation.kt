package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.CardExpiry
import com.aozijx.passly.domain.entry.model.credential.CardExpiryParseResult
import com.aozijx.passly.domain.entry.otp.OtpConfigValidation

data class EntryViolation(
    val field: FieldKey,
    val code: EntryViolationCode,
)

enum class EntryViolationCode {
    REQUIRED,
    INVALID_LENGTH,
    INVALID_FORMAT,
}

/** Pure domain validation; presentation maps violations to localized text. */
object EntryValidation {
    /** Partial updates validate only the replaced field; other sensitive values may be unrevealed. */
    internal fun validateEditedField(entry: Entry, field: FieldKey): Set<EntryViolation> = buildSet {
        when (field) {
            FieldKey.CARD_NUMBER -> validateCardNumber(entry.secret.card?.cardNumber)
            FieldKey.CARD_CVV -> validateCardCvv(entry.secret.card?.cardCvv)
            FieldKey.PASSWORD -> if (entry.secret.wifi != null) validateWifi(entry) else validateLogin(entry)
            else -> Unit
        }
    }

    fun validate(entry: Entry): Set<EntryViolation> = buildSet {
        when (entry.type) {
            EntryType.LOGIN,
            EntryType.DATABASE_CREDENTIAL,
            EntryType.SERVER_CREDENTIAL,
            EntryType.API_KEY,
            EntryType.CRYPTO_WALLET -> validateLogin(entry)

            EntryType.BANK_CARD -> validateCard(entry)
            EntryType.ID_CARD,
            EntryType.PASSPORT,
            EntryType.DRIVER_LICENSE -> validateIdentity(entry)

            EntryType.SSH_KEY -> validateSsh(entry)
            EntryType.WIFI -> validateWifi(entry)
            EntryType.PASSKEY -> requireValue(entry.secret.passkey?.privateKeyReference, FieldKey.PASSKEY_DATA)
            EntryType.OTP -> validateOtp(entry)
            EntryType.SEED_PHRASE -> validateSeedPhrase(entry)
            EntryType.RECOVERY_CODE -> validateRecoveryCodes(entry)
            EntryType.ACCOUNT,
            EntryType.NOTE -> Unit
        }
    }

    private fun MutableSet<EntryViolation>.validateLogin(entry: Entry) {
        requireValue(entry.secret.login?.password, FieldKey.PASSWORD)
    }

    private fun MutableSet<EntryViolation>.validateOtp(entry: Entry) {
        val config = entry.secret.otp?.config
        if (config == null) {
            invalid(FieldKey.OTP_SECRET, EntryViolationCode.REQUIRED)
            return
        }
        for (field in OtpConfigValidation.validate(config)) {
            val code = if (field == FieldKey.OTP_SECRET && config.secret.isNullOrBlank()) {
                EntryViolationCode.REQUIRED
            } else {
                EntryViolationCode.INVALID_FORMAT
            }
            invalid(field, code)
        }
    }

    private fun MutableSet<EntryViolation>.validateCard(entry: Entry) {
        val card = entry.secret.card ?: return
        validateCardNumber(card.cardNumber)
        validateCardCvv(card.cardCvv)
        card.cardExpiry?.let {
            if (CardExpiry.parse(it) !is CardExpiryParseResult.Parsed) {
                invalid(FieldKey.CARD_EXPIRATION, EntryViolationCode.INVALID_FORMAT)
            }
        }
    }

    private fun MutableSet<EntryViolation>.validateCardNumber(value: String?) {
        when (val number = CardNumberValidator.validate(value.orEmpty())) {
            CardNumberValidationResult.Empty -> invalid(FieldKey.CARD_NUMBER, EntryViolationCode.REQUIRED)
            is CardNumberValidationResult.Valid -> Unit
            is CardNumberValidationResult.Invalid -> invalid(FieldKey.CARD_NUMBER, when (number.reason) {
                CardNumberInvalidReason.InvalidLength -> EntryViolationCode.INVALID_LENGTH
                CardNumberInvalidReason.InvalidCharacters, CardNumberInvalidReason.ChecksumFailed -> EntryViolationCode.INVALID_FORMAT
            })
        }
    }

    private fun MutableSet<EntryViolation>.validateCardCvv(value: String?) {
        if (!CardCvvValidator.isValid(value)) {
            invalid(FieldKey.CARD_CVV, EntryViolationCode.INVALID_FORMAT)
        }
    }

    private fun MutableSet<EntryViolation>.validateIdentity(entry: Entry) {
        val value = entry.secret.identity?.idNumber
        requireValue(value, FieldKey.ID_NUMBER)
        if (value != null && value.length < 6) invalid(FieldKey.ID_NUMBER, EntryViolationCode.INVALID_LENGTH)
    }

    private fun MutableSet<EntryViolation>.validateSsh(entry: Entry) {
        val key = entry.secret.ssh?.privateKey
        requireValue(key, FieldKey.SSH_KEY)
        if (!key.isNullOrBlank() && "BEGIN" !in key) invalid(FieldKey.SSH_KEY, EntryViolationCode.INVALID_FORMAT)
    }

    private fun MutableSet<EntryViolation>.validateWifi(entry: Entry) {
        val wifi = entry.secret.wifi ?: return
        addAll(
            WifiCredentialValidation.validate(
                ssid = wifi.ssid,
                password = wifi.password,
                securityType = wifi.securityType,
            ),
        )
    }

    private fun MutableSet<EntryViolation>.validateSeedPhrase(entry: Entry) {
        val phrase = entry.secret.identity?.seedPhrase
        requireValue(phrase, FieldKey.SEED_PHRASE)
        if (!phrase.isNullOrBlank() && phrase.trim().split(Regex("\\s+")).size !in setOf(12, 24)) {
            invalid(FieldKey.SEED_PHRASE, EntryViolationCode.INVALID_LENGTH)
        }
    }

    private fun MutableSet<EntryViolation>.validateRecoveryCodes(entry: Entry) {
        val codes = entry.secret.identity?.recoveryCodes.orEmpty()
        if (codes.isEmpty()) invalid(FieldKey.RECOVERY_CODES, EntryViolationCode.REQUIRED)
        if (codes.any { it.length < 4 }) invalid(FieldKey.RECOVERY_CODES, EntryViolationCode.INVALID_LENGTH)
    }

    private fun MutableSet<EntryViolation>.requireValue(value: String?, field: FieldKey) {
        if (value.isNullOrBlank()) invalid(field, EntryViolationCode.REQUIRED)
    }

    private fun MutableSet<EntryViolation>.invalid(field: FieldKey, code: EntryViolationCode) {
        add(EntryViolation(field, code))
    }
}
