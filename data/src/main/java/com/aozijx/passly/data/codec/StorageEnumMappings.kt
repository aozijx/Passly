package com.aozijx.passly.data.codec

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.attachment.AttachmentStatus
import com.aozijx.passly.domain.entry.model.history.RevisionChange
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey

internal fun EntryType.toStorageKey(): String = when (this) {
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

internal fun entryTypeFromStorageKey(key: String): EntryType = when (key) {
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

internal fun ActivityType.toStorageKey(): String = when (this) {
    ActivityType.VIEW -> "VIEW"
    ActivityType.COPY_USERNAME -> "COPY_USERNAME"
    ActivityType.COPY_PASSWORD -> "COPY_PASSWORD"
    ActivityType.AUTOFILL -> "AUTOFILL"
    ActivityType.EXPORT -> "EXPORT"
    ActivityType.IMPORT -> "IMPORT"
    ActivityType.CREATE -> "CREATE"
    ActivityType.UPDATE -> "UPDATE"
    ActivityType.VERSION_RESTORED -> "VERSION_RESTORED"
    ActivityType.DELETE -> "DELETE"
    ActivityType.RESTORE -> "RESTORE"
}

internal fun activityTypeFromStorageKey(key: String): ActivityType = when (key) {
    "VIEW" -> ActivityType.VIEW
    "COPY_USERNAME" -> ActivityType.COPY_USERNAME
    "COPY_PASSWORD" -> ActivityType.COPY_PASSWORD
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

internal fun EntryRelationType.toStorageKey(): String = when (this) {
    EntryRelationType.MEMBER_OF_ACCOUNT -> "MEMBER_OF_ACCOUNT"
    EntryRelationType.OTP_FOR -> "OTP_FOR"
    EntryRelationType.RECOVERY_FOR -> "RECOVERY_FOR"
    EntryRelationType.RELATED_TO -> "RELATED_TO"
}

internal fun entryRelationTypeFromStorageKey(key: String): EntryRelationType = when (key) {
    "MEMBER_OF_ACCOUNT" -> EntryRelationType.MEMBER_OF_ACCOUNT
    "OTP_FOR" -> EntryRelationType.OTP_FOR
    "RECOVERY_FOR" -> EntryRelationType.RECOVERY_FOR
    "RELATED_TO" -> EntryRelationType.RELATED_TO
    else -> invalidStorageKey("entry relation type", key)
}

internal fun SensitiveFieldKey.toStorageKey(): String = when (this) {
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

internal fun sensitiveFieldKeyFromStorageKey(key: String): SensitiveFieldKey = when (key) {
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

fun AttachmentStatus.toStorageKey(): String = when (this) {
    AttachmentStatus.PENDING -> "PENDING"
    AttachmentStatus.COMMITTED -> "COMMITTED"
}

internal fun attachmentStatusFromStorageKey(key: String): AttachmentStatus = when (key) {
    "PENDING" -> AttachmentStatus.PENDING
    "COMMITTED" -> AttachmentStatus.COMMITTED
    else -> invalidStorageKey("attachment status", key)
}

internal fun RevisionChange.toStorageKey(): String = when (this) {
    RevisionChange.VALUE_CHANGED -> "VALUE_CHANGED"
    RevisionChange.VERSION_RESTORED -> "VERSION_RESTORED"
}

internal fun revisionChangeFromStorageKey(key: String): RevisionChange = when (key) {
    "VALUE_CHANGED" -> RevisionChange.VALUE_CHANGED
    "VERSION_RESTORED" -> RevisionChange.VERSION_RESTORED
    else -> invalidStorageKey("revision change", key)
}

internal fun OtpType.toStorageKey(): String = when (this) {
    OtpType.TOTP -> "TOTP"
    OtpType.HOTP -> "HOTP"
    OtpType.STEAM -> "STEAM"
}

internal fun otpTypeFromStorageKey(key: String): OtpType = when (key) {
    "TOTP" -> OtpType.TOTP
    "HOTP" -> OtpType.HOTP
    "STEAM" -> OtpType.STEAM
    else -> invalidStorageKey("OTP type", key)
}

private fun invalidStorageKey(kind: String, key: String): Nothing =
    throw IllegalArgumentException("Unknown $kind storage key: $key")
