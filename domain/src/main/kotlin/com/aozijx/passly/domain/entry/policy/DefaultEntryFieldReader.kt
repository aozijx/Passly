package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType

/** Reads existing scalar values without authorization, decryption or presentation formatting. */
class DefaultEntryFieldReader : EntryFieldReader {
    override fun getFieldValue(entry: Entry, key: FieldKey): String? {
        val profile = entry.profile
        val secret = entry.secret
        val otp = secret.otp?.config
        return when (key) {
            FieldKey.TITLE -> profile.title
            FieldKey.USERNAME -> profile.username
            FieldKey.EMAIL -> secret.login?.email
            FieldKey.PASSWORD -> secret.login?.password ?: secret.wifi?.password ?: secret.ssh?.passphrase
            FieldKey.NOTES -> secret.notes
            FieldKey.TAGS -> profile.tags.joinToString(", ")
            FieldKey.PRIMARY_URL -> profile.associations.primaryUrl
            FieldKey.DOMAINS -> profile.associations.domains.joinToString(", ")
            FieldKey.APPLICATION_IDS -> profile.associations.applicationIds.joinToString(", ")
            FieldKey.EXPIRES_AT -> profile.expiresAtMs?.toString()
            FieldKey.FAVORITE -> profile.favorite.toString()
            // A collection of named, typed fields has no single scalar value.
            FieldKey.CUSTOM_FIELDS -> null
            FieldKey.OTP_TYPE -> otp?.type?.let { type ->
                when (type) {
                    OtpType.TOTP -> "TOTP"
                    OtpType.HOTP -> "HOTP"
                    OtpType.STEAM -> "STEAM"
                }
            }
            FieldKey.OTP_SECRET -> otp?.secret
            FieldKey.OTP_ISSUER -> otp?.issuer
            FieldKey.OTP_ACCOUNT_NAME -> otp?.accountName
            FieldKey.OTP_PERIOD -> otp?.periodSeconds?.toString()
            FieldKey.OTP_COUNTER -> otp?.counter?.toString()
            FieldKey.OTP_DIGITS -> otp?.digits?.toString()
            FieldKey.OTP_ALGORITHM -> otp?.algorithm?.let { algorithm ->
                when (algorithm) {
                    OtpHashAlgorithm.SHA1 -> "SHA1"
                    OtpHashAlgorithm.SHA256 -> "SHA256"
                    OtpHashAlgorithm.SHA512 -> "SHA512"
                }
            }
            FieldKey.OTP_ENCODING -> otp?.encoding?.let { encoding ->
                when (encoding) {
                    OtpSecretEncoding.BASE32 -> "BASE32"
                    OtpSecretEncoding.BASE64 -> "BASE64"
                }
            }
            FieldKey.PASSKEY_CREDENTIAL_ID -> secret.passkey?.credentialId
            FieldKey.PASSKEY_RELYING_PARTY_ID -> secret.passkey?.rpId
            FieldKey.PASSKEY_USER_HANDLE -> secret.passkey?.userHandle
            FieldKey.PASSKEY_DATA -> secret.passkey?.privateKeyReference
            FieldKey.RECOVERY_CODES -> secret.identity?.recoveryCodes?.joinToString("\n")
            FieldKey.HARDWARE_INFO -> secret.passkey?.hardwareKeyInfo
            FieldKey.SSH_KEY -> secret.ssh?.privateKey
            FieldKey.SSH_PUBLIC_KEY -> secret.ssh?.publicKey
            FieldKey.SSH_PASSPHRASE -> secret.ssh?.passphrase
            FieldKey.SEED_PHRASE -> secret.identity?.seedPhrase
            FieldKey.CARD_TYPE -> secret.card?.cardType
            FieldKey.CARD_NUMBER -> secret.card?.cardNumber
            FieldKey.CARD_EXPIRATION -> secret.card?.cardExpiry
            FieldKey.CARD_CVV -> secret.card?.cardCvv
            FieldKey.CARD_HOLDER -> secret.card?.cardHolder
            FieldKey.CARD_ISSUING_BANK -> secret.card?.issuingBank
            FieldKey.CARD_BANK_BRANCH -> secret.card?.bankBranch
            FieldKey.CARD_PRODUCT -> secret.card?.cardProduct
            FieldKey.CARD_CURRENCY -> secret.card?.currency
            FieldKey.CARD_BANK_PHONE -> secret.card?.bankPhone
            FieldKey.PAYMENT_PIN -> secret.card?.paymentPin
            FieldKey.PAYMENT_PLATFORM -> secret.card?.paymentPlatform
            FieldKey.BILLING_ADDRESS -> secret.card?.billingAddress
            FieldKey.SECURITY_QUESTION -> secret.identity?.securityQuestion
            FieldKey.SECURITY_ANSWER -> secret.identity?.securityAnswer
            FieldKey.ID_NUMBER -> secret.identity?.idNumber
            FieldKey.WIFI_SSID -> secret.wifi?.ssid
            FieldKey.WIFI_SECURITY -> secret.wifi?.securityType?.storedValue
            FieldKey.WIFI_HIDDEN -> secret.wifi?.isHidden?.toString()
        }
    }
}
