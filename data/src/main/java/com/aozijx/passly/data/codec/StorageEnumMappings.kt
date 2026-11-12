package com.aozijx.passly.data.codec

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.history.RevisionChange
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey

internal fun EntryType.toStorageKey(): String =
    when (this) {
        EntryType.ACCOUNT -> "ACCOUNT"
        EntryType.LOGIN -> "LOGIN"
        EntryType.NOTE -> "NOTE"
        EntryType.BANK_CARD -> "BANK_CARD"
        EntryType.ID_CARD -> "ID_CARD"
        EntryType.PASSPORT -> "PASSPORT"
        EntryType.DRIVER_LICENSE -> "DRIVER_LICENSE"
        EntryType.SSH_KEY -> "SSH_KEY"
        EntryType.WIFI -> "WIFI"
        EntryType.PASSKEY -> "PASSKEY"
        EntryType.OTP -> "OTP"
        EntryType.DATABASE_CREDENTIAL -> "DATABASE_CREDENTIAL"
        EntryType.SERVER_CREDENTIAL -> "SERVER_CREDENTIAL"
        EntryType.API_KEY -> "API_KEY"
        EntryType.CRYPTO_WALLET -> "CRYPTO_WALLET"
        EntryType.SEED_PHRASE -> "SEED_PHRASE"
        EntryType.RECOVERY_CODE -> "RECOVERY_CODE"
    }

internal fun entryTypeFromStorageKey(key: String): EntryType =
    when (key) {
        "ACCOUNT" -> EntryType.ACCOUNT
        "LOGIN" -> EntryType.LOGIN
        "NOTE" -> EntryType.NOTE
        "BANK_CARD" -> EntryType.BANK_CARD
        "ID_CARD" -> EntryType.ID_CARD
        "PASSPORT" -> EntryType.PASSPORT
        "DRIVER_LICENSE" -> EntryType.DRIVER_LICENSE
        "SSH_KEY" -> EntryType.SSH_KEY
        "WIFI" -> EntryType.WIFI
        "PASSKEY" -> EntryType.PASSKEY
        "OTP" -> EntryType.OTP
        "DATABASE_CREDENTIAL" -> EntryType.DATABASE_CREDENTIAL
        "SERVER_CREDENTIAL" -> EntryType.SERVER_CREDENTIAL
        "API_KEY" -> EntryType.API_KEY
        "CRYPTO_WALLET" -> EntryType.CRYPTO_WALLET
        "SEED_PHRASE" -> EntryType.SEED_PHRASE
        "RECOVERY_CODE" -> EntryType.RECOVERY_CODE
        else -> invalidStorageKey("entry type", key)
    }

internal fun ActivityType.toStorageKey(): String =
    when (this) {
        ActivityType.VIEW -> "VIEW"
        ActivityType.REVEAL_FIELDS -> "REVEAL_FIELDS"
        ActivityType.COPY_FIELD -> "COPY_FIELD"
        ActivityType.COPY_OTP_CODE -> "COPY_OTP_CODE"
        ActivityType.AUTOFILL -> "AUTOFILL"
        ActivityType.EXPORT -> "EXPORT"
        ActivityType.IMPORT -> "IMPORT"
        ActivityType.CREATE -> "CREATE"
        ActivityType.UPDATE -> "UPDATE"
        ActivityType.VERSION_RESTORED -> "VERSION_RESTORED"
        ActivityType.DELETE -> "DELETE"
        ActivityType.RESTORE -> "RESTORE"
    }

internal fun activityTypeFromStorageKey(key: String): ActivityType =
    when (key) {
        "VIEW" -> ActivityType.VIEW
        "REVEAL_FIELDS" -> ActivityType.REVEAL_FIELDS
        "COPY_FIELD" -> ActivityType.COPY_FIELD
        "COPY_OTP_CODE" -> ActivityType.COPY_OTP_CODE
        "AUTOFILL" -> ActivityType.AUTOFILL
        "EXPORT" -> ActivityType.EXPORT
        "IMPORT" -> ActivityType.IMPORT
        "CREATE" -> ActivityType.CREATE
        "UPDATE" -> ActivityType.UPDATE
        "VERSION_RESTORED" -> ActivityType.VERSION_RESTORED
        "DELETE" -> ActivityType.DELETE
        "RESTORE" -> ActivityType.RESTORE
        else -> invalidStorageKey("activity type", key)
    }

internal fun FieldKey.toStorageKey(): String =
    when (this) {
        FieldKey.TITLE -> "TITLE"
        FieldKey.USERNAME -> "USERNAME"
        FieldKey.EMAIL -> "EMAIL"
        FieldKey.PASSWORD -> "PASSWORD"
        FieldKey.NOTES -> "NOTES"
        FieldKey.TAGS -> "TAGS"
        FieldKey.PRIMARY_URL -> "PRIMARY_URL"
        FieldKey.DOMAINS -> "DOMAINS"
        FieldKey.APPLICATION_IDS -> "APPLICATION_IDS"
        FieldKey.EXPIRES_AT -> "EXPIRES_AT"
        FieldKey.FAVORITE -> "FAVORITE"
        FieldKey.CUSTOM_FIELDS -> "CUSTOM_FIELDS"
        FieldKey.OTP_TYPE -> "OTP_TYPE"
        FieldKey.OTP_SECRET -> "OTP_SECRET"
        FieldKey.OTP_ISSUER -> "OTP_ISSUER"
        FieldKey.OTP_ACCOUNT_NAME -> "OTP_ACCOUNT_NAME"
        FieldKey.OTP_PERIOD -> "OTP_PERIOD"
        FieldKey.OTP_COUNTER -> "OTP_COUNTER"
        FieldKey.OTP_DIGITS -> "OTP_DIGITS"
        FieldKey.OTP_ALGORITHM -> "OTP_ALGORITHM"
        FieldKey.OTP_ENCODING -> "OTP_ENCODING"
        FieldKey.PASSKEY_CREDENTIAL_ID -> "PASSKEY_CREDENTIAL_ID"
        FieldKey.PASSKEY_RELYING_PARTY_ID -> "PASSKEY_RELYING_PARTY_ID"
        FieldKey.PASSKEY_USER_HANDLE -> "PASSKEY_USER_HANDLE"
        FieldKey.PASSKEY_DATA -> "PASSKEY_DATA"
        FieldKey.RECOVERY_CODES -> "RECOVERY_CODES"
        FieldKey.HARDWARE_INFO -> "HARDWARE_INFO"
        FieldKey.SSH_KEY -> "SSH_KEY"
        FieldKey.SSH_PUBLIC_KEY -> "SSH_PUBLIC_KEY"
        FieldKey.SSH_PASSPHRASE -> "SSH_PASSPHRASE"
        FieldKey.SEED_PHRASE -> "SEED_PHRASE"
        FieldKey.CARD_TYPE -> "CARD_TYPE"
        FieldKey.CARD_NUMBER -> "CARD_NUMBER"
        FieldKey.CARD_EXPIRATION -> "CARD_EXPIRATION"
        FieldKey.CARD_CVV -> "CARD_CVV"
        FieldKey.CARD_HOLDER -> "CARD_HOLDER"
        FieldKey.CARD_ISSUING_BANK -> "CARD_ISSUING_BANK"
        FieldKey.CARD_BANK_BRANCH -> "CARD_BANK_BRANCH"
        FieldKey.CARD_PRODUCT -> "CARD_PRODUCT"
        FieldKey.CARD_CURRENCY -> "CARD_CURRENCY"
        FieldKey.CARD_BANK_PHONE -> "CARD_BANK_PHONE"
        FieldKey.PAYMENT_PIN -> "PAYMENT_PIN"
        FieldKey.PAYMENT_PLATFORM -> "PAYMENT_PLATFORM"
        FieldKey.BILLING_ADDRESS -> "BILLING_ADDRESS"
        FieldKey.SECURITY_QUESTION -> "SECURITY_QUESTION"
        FieldKey.SECURITY_ANSWER -> "SECURITY_ANSWER"
        FieldKey.ID_NUMBER -> "ID_NUMBER"
        FieldKey.WIFI_SSID -> "WIFI_SSID"
        FieldKey.WIFI_SECURITY -> "WIFI_SECURITY"
        FieldKey.WIFI_HIDDEN -> "WIFI_HIDDEN"
    }

internal fun fieldKeyFromStorageKey(key: String): FieldKey =
    when (key) {
        "TITLE" -> FieldKey.TITLE
        "USERNAME" -> FieldKey.USERNAME
        "EMAIL" -> FieldKey.EMAIL
        "PASSWORD" -> FieldKey.PASSWORD
        "NOTES" -> FieldKey.NOTES
        "TAGS" -> FieldKey.TAGS
        "PRIMARY_URL" -> FieldKey.PRIMARY_URL
        "DOMAINS" -> FieldKey.DOMAINS
        "APPLICATION_IDS" -> FieldKey.APPLICATION_IDS
        "EXPIRES_AT" -> FieldKey.EXPIRES_AT
        "FAVORITE" -> FieldKey.FAVORITE
        "CUSTOM_FIELDS" -> FieldKey.CUSTOM_FIELDS
        "OTP_TYPE" -> FieldKey.OTP_TYPE
        "OTP_SECRET" -> FieldKey.OTP_SECRET
        "OTP_ISSUER" -> FieldKey.OTP_ISSUER
        "OTP_ACCOUNT_NAME" -> FieldKey.OTP_ACCOUNT_NAME
        "OTP_PERIOD" -> FieldKey.OTP_PERIOD
        "OTP_COUNTER" -> FieldKey.OTP_COUNTER
        "OTP_DIGITS" -> FieldKey.OTP_DIGITS
        "OTP_ALGORITHM" -> FieldKey.OTP_ALGORITHM
        "OTP_ENCODING" -> FieldKey.OTP_ENCODING
        "PASSKEY_CREDENTIAL_ID" -> FieldKey.PASSKEY_CREDENTIAL_ID
        "PASSKEY_RELYING_PARTY_ID" -> FieldKey.PASSKEY_RELYING_PARTY_ID
        "PASSKEY_USER_HANDLE" -> FieldKey.PASSKEY_USER_HANDLE
        "PASSKEY_DATA" -> FieldKey.PASSKEY_DATA
        "RECOVERY_CODES" -> FieldKey.RECOVERY_CODES
        "HARDWARE_INFO" -> FieldKey.HARDWARE_INFO
        "SSH_KEY" -> FieldKey.SSH_KEY
        "SSH_PUBLIC_KEY" -> FieldKey.SSH_PUBLIC_KEY
        "SSH_PASSPHRASE" -> FieldKey.SSH_PASSPHRASE
        "SEED_PHRASE" -> FieldKey.SEED_PHRASE
        "CARD_TYPE" -> FieldKey.CARD_TYPE
        "CARD_NUMBER" -> FieldKey.CARD_NUMBER
        "CARD_EXPIRATION" -> FieldKey.CARD_EXPIRATION
        "CARD_CVV" -> FieldKey.CARD_CVV
        "CARD_HOLDER" -> FieldKey.CARD_HOLDER
        "CARD_ISSUING_BANK" -> FieldKey.CARD_ISSUING_BANK
        "CARD_BANK_BRANCH" -> FieldKey.CARD_BANK_BRANCH
        "CARD_PRODUCT" -> FieldKey.CARD_PRODUCT
        "CARD_CURRENCY" -> FieldKey.CARD_CURRENCY
        "CARD_BANK_PHONE" -> FieldKey.CARD_BANK_PHONE
        "PAYMENT_PIN" -> FieldKey.PAYMENT_PIN
        "PAYMENT_PLATFORM" -> FieldKey.PAYMENT_PLATFORM
        "BILLING_ADDRESS" -> FieldKey.BILLING_ADDRESS
        "SECURITY_QUESTION" -> FieldKey.SECURITY_QUESTION
        "SECURITY_ANSWER" -> FieldKey.SECURITY_ANSWER
        "ID_NUMBER" -> FieldKey.ID_NUMBER
        "WIFI_SSID" -> FieldKey.WIFI_SSID
        "WIFI_SECURITY" -> FieldKey.WIFI_SECURITY
        "WIFI_HIDDEN" -> FieldKey.WIFI_HIDDEN
        else -> invalidStorageKey("field key", key)
    }

internal fun EntryRelationType.toStorageKey(): String =
    when (this) {
        EntryRelationType.MEMBER_OF_ACCOUNT -> "MEMBER_OF_ACCOUNT"
        EntryRelationType.OTP_FOR -> "OTP_FOR"
        EntryRelationType.RECOVERY_FOR -> "RECOVERY_FOR"
        EntryRelationType.RELATED_TO -> "RELATED_TO"
    }

internal fun entryRelationTypeFromStorageKey(key: String): EntryRelationType =
    when (key) {
        "MEMBER_OF_ACCOUNT" -> EntryRelationType.MEMBER_OF_ACCOUNT
        "OTP_FOR" -> EntryRelationType.OTP_FOR
        "RECOVERY_FOR" -> EntryRelationType.RECOVERY_FOR
        "RELATED_TO" -> EntryRelationType.RELATED_TO
        else -> invalidStorageKey("entry relation type", key)
    }

internal fun SensitiveFieldKey.toStorageKey(): String =
    when (this) {
        SensitiveFieldKey.PASSWORD -> "PASSWORD"
        SensitiveFieldKey.CARD_NUMBER -> "CARD_NUMBER"
        SensitiveFieldKey.CARD_CVV -> "CARD_CVV"
        SensitiveFieldKey.CARD_PAYMENT_PIN -> "CARD_PAYMENT_PIN"
        SensitiveFieldKey.IDENTITY_NUMBER -> "IDENTITY_NUMBER"
        SensitiveFieldKey.SEED_PHRASE -> "SEED_PHRASE"
        SensitiveFieldKey.RECOVERY_CODES -> "RECOVERY_CODES"
        SensitiveFieldKey.SSH_PRIVATE_KEY -> "SSH_PRIVATE_KEY"
        SensitiveFieldKey.SSH_PASSPHRASE -> "SSH_PASSPHRASE"
        SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE -> "PASSKEY_PRIVATE_REFERENCE"
        SensitiveFieldKey.OTP_SECRET -> "OTP_SECRET"
    }

internal fun sensitiveFieldKeyFromStorageKey(key: String): SensitiveFieldKey =
    when (key) {
        "PASSWORD" -> SensitiveFieldKey.PASSWORD
        "CARD_NUMBER" -> SensitiveFieldKey.CARD_NUMBER
        "CARD_CVV" -> SensitiveFieldKey.CARD_CVV
        "CARD_PAYMENT_PIN" -> SensitiveFieldKey.CARD_PAYMENT_PIN
        "IDENTITY_NUMBER" -> SensitiveFieldKey.IDENTITY_NUMBER
        "SEED_PHRASE" -> SensitiveFieldKey.SEED_PHRASE
        "RECOVERY_CODES" -> SensitiveFieldKey.RECOVERY_CODES
        "SSH_PRIVATE_KEY" -> SensitiveFieldKey.SSH_PRIVATE_KEY
        "SSH_PASSPHRASE" -> SensitiveFieldKey.SSH_PASSPHRASE
        "PASSKEY_PRIVATE_REFERENCE" -> SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE
        "OTP_SECRET" -> SensitiveFieldKey.OTP_SECRET
        else -> invalidStorageKey("sensitive field", key)
    }

internal fun RevisionChange.toStorageKey(): String =
    when (this) {
        RevisionChange.VALUE_CHANGED -> "VALUE_CHANGED"
        RevisionChange.VERSION_RESTORED -> "VERSION_RESTORED"
    }

internal fun revisionChangeFromStorageKey(key: String): RevisionChange =
    when (key) {
        "VALUE_CHANGED" -> RevisionChange.VALUE_CHANGED
        "VERSION_RESTORED" -> RevisionChange.VERSION_RESTORED
        else -> invalidStorageKey("revision change", key)
    }

internal fun OtpType.toStorageKey(): String =
    when (this) {
        OtpType.TOTP -> "TOTP"
        OtpType.HOTP -> "HOTP"
        OtpType.STEAM -> "STEAM"
    }

internal fun otpTypeFromStorageKey(key: String): OtpType =
    when (key) {
        "TOTP" -> OtpType.TOTP
        "HOTP" -> OtpType.HOTP
        "STEAM" -> OtpType.STEAM
        else -> invalidStorageKey("OTP type", key)
    }

internal fun OtpHashAlgorithm.toStorageKey(): String =
    when (this) {
        OtpHashAlgorithm.SHA1 -> "SHA1"
        OtpHashAlgorithm.SHA256 -> "SHA256"
        OtpHashAlgorithm.SHA512 -> "SHA512"
    }

internal fun otpHashAlgorithmFromStorageKey(key: String): OtpHashAlgorithm =
    when (key) {
        "SHA1" -> OtpHashAlgorithm.SHA1
        "SHA256" -> OtpHashAlgorithm.SHA256
        "SHA512" -> OtpHashAlgorithm.SHA512
        else -> invalidStorageKey("OTP hash algorithm", key)
    }

internal fun OtpSecretEncoding.toStorageKey(): String =
    when (this) {
        OtpSecretEncoding.BASE32 -> "BASE32"
        OtpSecretEncoding.BASE64 -> "BASE64"
    }

internal fun otpSecretEncodingFromStorageKey(key: String): OtpSecretEncoding =
    when (key) {
        "BASE32" -> OtpSecretEncoding.BASE32
        "BASE64" -> OtpSecretEncoding.BASE64
        else -> invalidStorageKey("OTP secret encoding", key)
    }

private fun invalidStorageKey(
    kind: String,
    key: String,
): Nothing = throw IllegalArgumentException("Unknown $kind storage key: $key")
