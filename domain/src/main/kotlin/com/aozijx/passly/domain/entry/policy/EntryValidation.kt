package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey

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
            EntryType.OTP -> requireValue(entry.secret.otp?.config?.secret, FieldKey.OTP_SECRET)
            EntryType.SEED_PHRASE -> validateSeedPhrase(entry)
            EntryType.RECOVERY_CODE -> validateRecoveryCodes(entry)
            EntryType.ACCOUNT,
            EntryType.NOTE -> Unit
        }
    }

    private fun MutableSet<EntryViolation>.validateLogin(entry: Entry) {
        requireValue(entry.profile.username, FieldKey.USERNAME)
        requireValue(entry.secret.login?.password, FieldKey.PASSWORD)
    }

    private fun MutableSet<EntryViolation>.validateCard(entry: Entry) {
        val card = entry.secret.card ?: return
        requireValue(card.cardNumber, FieldKey.CARD_NUMBER)
        card.cardNumber?.filter(Char::isDigit)?.takeIf(String::isNotEmpty)?.let {
            if (it.length !in 13..19) invalid(FieldKey.CARD_NUMBER, EntryViolationCode.INVALID_LENGTH)
        }
        card.cardExpiry?.let {
            if (!it.matches(Regex("^\\d{2}/\\d{2}$"))) invalid(FieldKey.CARD_EXPIRATION, EntryViolationCode.INVALID_FORMAT)
        }
        card.cardCvv?.let {
            if (!it.matches(Regex("^\\d{3,4}$"))) invalid(FieldKey.CARD_CVV, EntryViolationCode.INVALID_FORMAT)
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
        val password = entry.secret.wifi?.password
        requireValue(password, FieldKey.PASSWORD)
        if (!password.isNullOrEmpty() && password.length < 8) invalid(FieldKey.PASSWORD, EntryViolationCode.INVALID_LENGTH)
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
