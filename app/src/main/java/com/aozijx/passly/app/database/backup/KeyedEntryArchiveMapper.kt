package com.aozijx.passly.app.database.backup

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.EntryIcon
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.CustomField
import com.aozijx.passly.domain.entry.model.credential.CustomFieldKind
import com.aozijx.passly.domain.entry.model.credential.EntryCredential
import com.aozijx.passly.domain.entry.model.credential.EntryCredentialKind
import com.aozijx.passly.domain.entry.model.credential.IdentityCredential
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.credential.PasskeyCredential
import com.aozijx.passly.domain.entry.model.credential.SshCredential
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.feature.backup.internal.archive.BackupArchiveKeyRegistry
import com.aozijx.passly.feature.backup.internal.archive.model.BackupCustomFieldValue
import com.aozijx.passly.feature.backup.internal.archive.model.BackupEntryRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldValue

internal object KeyedEntryArchiveMapper {
    fun toRecord(entry: Entry, attachmentIds: List<String>): BackupEntryRecord = BackupEntryRecord(
        id = entry.id.value,
        type = BackupArchiveKeyRegistry.entryTypeKey(entry.type),
        revision = entry.version.value,
        createdAt = entry.timestamps.createdAtMs,
        updatedAt = entry.timestamps.updatedAtMs,
        deletedAt = entry.timestamps.deletedAtMs,
        fields = entry.toArchiveFields(),
        attachmentIds = attachmentIds,
    )

    fun toEntry(record: BackupEntryRecord): Entry {
        val type = requireNotNull(BackupArchiveKeyRegistry.entryType(record.type)) {
            "Unknown archive entry type: ${record.type}"
        }
        val fields = record.fields.toArchiveFieldMap()
        val credential = when (type.credentialKind) {
            EntryCredentialKind.NONE -> EntryCredential.None
            EntryCredentialKind.LOGIN -> LoginCredential(
                email = fields.text(FieldKey.EMAIL),
                password = fields.text(FieldKey.PASSWORD),
            )
            EntryCredentialKind.CARD -> CardCredential(
                cardType = fields.text(FieldKey.CARD_TYPE),
                cardNumber = fields.text(FieldKey.CARD_NUMBER),
                cardExpiry = fields.text(FieldKey.CARD_EXPIRATION),
                cardCvv = fields.text(FieldKey.CARD_CVV),
                cardHolder = fields.text(FieldKey.CARD_HOLDER),
                paymentPin = fields.text(FieldKey.PAYMENT_PIN),
                paymentPlatform = fields.text(FieldKey.PAYMENT_PLATFORM),
                billingAddress = fields.text(FieldKey.BILLING_ADDRESS),
            )
            EntryCredentialKind.IDENTITY -> IdentityCredential(
                idNumber = fields.text(FieldKey.ID_NUMBER),
                securityQuestion = fields.text(FieldKey.SECURITY_QUESTION),
                securityAnswer = fields.text(FieldKey.SECURITY_ANSWER),
                seedPhrase = fields.text(FieldKey.SEED_PHRASE),
                recoveryCodes = fields.texts(FieldKey.RECOVERY_CODES),
            )
            EntryCredentialKind.SSH -> SshCredential(
                privateKey = fields.text(FieldKey.SSH_KEY),
                publicKey = fields.text(FieldKey.SSH_PUBLIC_KEY),
                passphrase = fields.text(FieldKey.SSH_PASSPHRASE),
            )
            EntryCredentialKind.WIFI -> WifiCredential(
                ssid = requireNotNull(fields.text(FieldKey.WIFI_SSID)) {
                    "Wi-Fi archive entry is missing wifi_ssid"
                },
                password = fields.text(FieldKey.PASSWORD),
                securityType = fields.text(FieldKey.WIFI_SECURITY),
                isHidden = fields.boolean(FieldKey.WIFI_HIDDEN) ?: false,
            )
            EntryCredentialKind.PASSKEY -> PasskeyCredential(
                credentialId = fields.text(FieldKey.PASSKEY_CREDENTIAL_ID),
                rpId = fields.text(FieldKey.PASSKEY_RELYING_PARTY_ID),
                userHandle = fields.text(FieldKey.PASSKEY_USER_HANDLE),
                privateKeyReference = fields.text(FieldKey.PASSKEY_DATA),
                hardwareKeyInfo = fields.text(FieldKey.HARDWARE_INFO),
            )
            EntryCredentialKind.OTP -> OtpCredential(
                OtpConfig(
                    type = fields.text(FieldKey.OTP_TYPE).toOtpType(),
                    secret = fields.text(FieldKey.OTP_SECRET),
                    algorithm = fields.text(FieldKey.OTP_ALGORITHM).toOtpAlgorithm(),
                    digits = fields.integer(FieldKey.OTP_DIGITS) ?: 6,
                    periodSeconds = fields.integer(FieldKey.OTP_PERIOD),
                    counter = fields.long(FieldKey.OTP_COUNTER),
                    encoding = fields.text(FieldKey.OTP_ENCODING).toOtpEncoding(),
                    issuer = fields.text(FieldKey.OTP_ISSUER),
                    accountName = fields.text(FieldKey.OTP_ACCOUNT_NAME),
                ),
            )
        }
        return Entry(
            identity = EntryIdentity(
                id = EntryId(record.id),
                type = type,
                version = EntryVersion(record.revision),
                timestamps = EntryTimestamps(record.createdAt, record.updatedAt, record.deletedAt),
            ),
            profile = EntryProfile(
                title = requireNotNull(fields.text(FieldKey.TITLE)) {
                    "Archive entry is missing title"
                },
                username = fields.text(FieldKey.USERNAME).orEmpty(),
                associations = EntryAssociations(
                    primaryUrl = fields.text(FieldKey.PRIMARY_URL),
                    domains = fields.texts(FieldKey.DOMAINS).toCollection(linkedSetOf()),
                    applicationIds = fields.texts(FieldKey.APPLICATION_IDS).toCollection(linkedSetOf()),
                ),
                icon = EntryIcon(
                    name = fields[ICON_NAME_KEY]?.requiredText(),
                    color = fields[ICON_COLOR_KEY]?.requiredText(),
                ),
                favorite = fields.boolean(FieldKey.FAVORITE) ?: false,
                tags = fields.texts(FieldKey.TAGS).toCollection(linkedSetOf()),
                expiresAtMs = fields.long(FieldKey.EXPIRES_AT),
            ),
            secret = EntrySecret(
                credential = credential,
                notes = fields.text(FieldKey.NOTES),
                customFields = fields.customFields(),
            ),
        )
    }
}

private fun Entry.toArchiveFields(): List<BackupFieldRecord> = buildList {
    fun put(key: FieldKey, value: BackupFieldValue?) {
        if (value != null) add(BackupFieldRecord(BackupArchiveKeyRegistry.fieldKey(key), value))
    }
    fun putText(key: FieldKey, value: String?) {
        value?.takeUnless(String::isBlank)?.let { put(key, BackupFieldValue.text(it)) }
    }
    fun putTexts(key: FieldKey, values: Collection<String>) {
        if (values.isNotEmpty()) put(key, BackupFieldValue.texts(values.toList()))
    }

    put(FieldKey.TITLE, BackupFieldValue.text(profile.title))
    putText(FieldKey.USERNAME, profile.username)
    putText(FieldKey.PRIMARY_URL, profile.associations.primaryUrl)
    putTexts(FieldKey.DOMAINS, profile.associations.domains)
    putTexts(FieldKey.APPLICATION_IDS, profile.associations.applicationIds)
    put(FieldKey.FAVORITE, BackupFieldValue.boolean(profile.favorite))
    putTexts(FieldKey.TAGS, profile.tags)
    profile.expiresAtMs?.let { put(FieldKey.EXPIRES_AT, BackupFieldValue.long(it)) }
    profile.icon.name?.takeUnless(String::isBlank)?.let {
        add(BackupFieldRecord(ICON_NAME_KEY, BackupFieldValue.text(it)))
    }
    profile.icon.color?.takeUnless(String::isBlank)?.let {
        add(BackupFieldRecord(ICON_COLOR_KEY, BackupFieldValue.text(it)))
    }

    when (val credential = secret.credential) {
        EntryCredential.None -> Unit
        is LoginCredential -> {
            putText(FieldKey.EMAIL, credential.email)
            putText(FieldKey.PASSWORD, credential.password)
        }
        is CardCredential -> {
            putText(FieldKey.CARD_TYPE, credential.cardType)
            putText(FieldKey.CARD_NUMBER, credential.cardNumber)
            putText(FieldKey.CARD_EXPIRATION, credential.cardExpiry)
            putText(FieldKey.CARD_CVV, credential.cardCvv)
            putText(FieldKey.CARD_HOLDER, credential.cardHolder)
            putText(FieldKey.PAYMENT_PIN, credential.paymentPin)
            putText(FieldKey.PAYMENT_PLATFORM, credential.paymentPlatform)
            putText(FieldKey.BILLING_ADDRESS, credential.billingAddress)
        }
        is IdentityCredential -> {
            putText(FieldKey.ID_NUMBER, credential.idNumber)
            putText(FieldKey.SECURITY_QUESTION, credential.securityQuestion)
            putText(FieldKey.SECURITY_ANSWER, credential.securityAnswer)
            putText(FieldKey.SEED_PHRASE, credential.seedPhrase)
            putTexts(FieldKey.RECOVERY_CODES, credential.recoveryCodes)
        }
        is SshCredential -> {
            putText(FieldKey.SSH_KEY, credential.privateKey)
            putText(FieldKey.SSH_PUBLIC_KEY, credential.publicKey)
            putText(FieldKey.SSH_PASSPHRASE, credential.passphrase)
        }
        is WifiCredential -> {
            put(FieldKey.WIFI_SSID, BackupFieldValue.text(credential.ssid))
            putText(FieldKey.PASSWORD, credential.password)
            putText(FieldKey.WIFI_SECURITY, credential.securityType)
            put(FieldKey.WIFI_HIDDEN, BackupFieldValue.boolean(credential.isHidden))
        }
        is PasskeyCredential -> {
            putText(FieldKey.PASSKEY_CREDENTIAL_ID, credential.credentialId)
            putText(FieldKey.PASSKEY_RELYING_PARTY_ID, credential.rpId)
            putText(FieldKey.PASSKEY_USER_HANDLE, credential.userHandle)
            putText(FieldKey.PASSKEY_DATA, credential.privateKeyReference)
            putText(FieldKey.HARDWARE_INFO, credential.hardwareKeyInfo)
        }
        is OtpCredential -> with(credential.config) {
            put(FieldKey.OTP_TYPE, BackupFieldValue.text(type.archiveKey()))
            putText(FieldKey.OTP_SECRET, secret)
            put(FieldKey.OTP_ALGORITHM, BackupFieldValue.text(algorithm.archiveKey()))
            put(FieldKey.OTP_DIGITS, BackupFieldValue.integer(digits))
            periodSeconds?.let { put(FieldKey.OTP_PERIOD, BackupFieldValue.integer(it)) }
            counter?.let { put(FieldKey.OTP_COUNTER, BackupFieldValue.long(it)) }
            put(FieldKey.OTP_ENCODING, BackupFieldValue.text(encoding.archiveKey()))
            putText(FieldKey.OTP_ISSUER, issuer)
            putText(FieldKey.OTP_ACCOUNT_NAME, accountName)
        }
    }
    putText(FieldKey.NOTES, secret.notes)
    if (secret.customFields.isNotEmpty()) {
        put(
            FieldKey.CUSTOM_FIELDS,
            BackupFieldValue.customFields(
                secret.customFields.map { field ->
                    BackupCustomFieldValue(
                        name = field.name,
                        value = field.value,
                        kind = when (field.kind) {
                            CustomFieldKind.TEXT -> BackupCustomFieldValue.KIND_TEXT
                            CustomFieldKind.HIDDEN -> BackupCustomFieldValue.KIND_HIDDEN
                        },
                    )
                },
            ),
        )
    }
}

private fun List<BackupFieldRecord>.toArchiveFieldMap(): Map<String, BackupFieldValue> {
    require(map(BackupFieldRecord::key).distinct().size == size) {
        "Archive entry contains duplicate field keys"
    }
    return associate { it.key to it.value }
}

private fun Map<String, BackupFieldValue>.value(key: FieldKey): BackupFieldValue? =
    this[BackupArchiveKeyRegistry.fieldKey(key)]
private fun Map<String, BackupFieldValue>.text(key: FieldKey): String? = value(key)?.requiredText()
private fun Map<String, BackupFieldValue>.texts(key: FieldKey): List<String> =
    value(key)?.requiredTexts().orEmpty()
private fun Map<String, BackupFieldValue>.boolean(key: FieldKey): Boolean? = value(key)?.requiredBoolean()
private fun Map<String, BackupFieldValue>.integer(key: FieldKey): Int? = value(key)?.requiredInteger()
private fun Map<String, BackupFieldValue>.long(key: FieldKey): Long? = value(key)?.requiredLong()

private fun Map<String, BackupFieldValue>.customFields(): List<CustomField> =
    value(FieldKey.CUSTOM_FIELDS)?.requiredCustomFields().orEmpty().map { field ->
        CustomField(
            name = field.name,
            value = field.value,
            kind = when (field.kind) {
                BackupCustomFieldValue.KIND_TEXT -> CustomFieldKind.TEXT
                BackupCustomFieldValue.KIND_HIDDEN -> CustomFieldKind.HIDDEN
                else -> throw IllegalArgumentException("Unknown custom field kind: ${field.kind}")
            },
        )
    }

private fun BackupFieldValue.requiredText(): String = requireNotNull(text) {
    "Field kind $kind does not contain text"
}.also { require(kind == BackupFieldValue.KIND_TEXT) { "Expected text field, found $kind" } }
private fun BackupFieldValue.requiredTexts(): List<String> = requireNotNull(texts) {
    "Field kind $kind does not contain texts"
}.also { require(kind == BackupFieldValue.KIND_TEXT_LIST) { "Expected text_list field, found $kind" } }
private fun BackupFieldValue.requiredBoolean(): Boolean = requireNotNull(booleanValue) {
    "Field kind $kind does not contain boolean"
}.also { require(kind == BackupFieldValue.KIND_BOOLEAN) { "Expected boolean field, found $kind" } }
private fun BackupFieldValue.requiredInteger(): Int = requireNotNull(integerValue) {
    "Field kind $kind does not contain integer"
}.also { require(kind == BackupFieldValue.KIND_INTEGER) { "Expected integer field, found $kind" } }
private fun BackupFieldValue.requiredLong(): Long = requireNotNull(longValue) {
    "Field kind $kind does not contain long"
}.also { require(kind == BackupFieldValue.KIND_LONG) { "Expected long field, found $kind" } }
private fun BackupFieldValue.requiredCustomFields(): List<BackupCustomFieldValue> =
    requireNotNull(customFields) { "Field kind $kind does not contain custom fields" }.also {
        require(kind == BackupFieldValue.KIND_CUSTOM_FIELDS) {
            "Expected custom_fields field, found $kind"
        }
    }

private fun OtpType.archiveKey(): String = when (this) {
    OtpType.TOTP -> "totp"
    OtpType.HOTP -> "hotp"
    OtpType.STEAM -> "steam"
}
private fun String?.toOtpType(): OtpType = when (this) {
    null, "totp" -> OtpType.TOTP
    "hotp" -> OtpType.HOTP
    "steam" -> OtpType.STEAM
    else -> throw IllegalArgumentException("Unknown OTP type: $this")
}
private fun OtpHashAlgorithm.archiveKey(): String = when (this) {
    OtpHashAlgorithm.SHA1 -> "sha1"
    OtpHashAlgorithm.SHA256 -> "sha256"
    OtpHashAlgorithm.SHA512 -> "sha512"
}
private fun String?.toOtpAlgorithm(): OtpHashAlgorithm = when (this) {
    null, "sha1" -> OtpHashAlgorithm.SHA1
    "sha256" -> OtpHashAlgorithm.SHA256
    "sha512" -> OtpHashAlgorithm.SHA512
    else -> throw IllegalArgumentException("Unknown OTP algorithm: $this")
}
private fun OtpSecretEncoding.archiveKey(): String = when (this) {
    OtpSecretEncoding.BASE32 -> "base32"
    OtpSecretEncoding.BASE64 -> "base64"
}
private fun String?.toOtpEncoding(): OtpSecretEncoding = when (this) {
    null, "base32" -> OtpSecretEncoding.BASE32
    "base64" -> OtpSecretEncoding.BASE64
    else -> throw IllegalArgumentException("Unknown OTP encoding: $this")
}

private const val ICON_NAME_KEY = "icon_name"
private const val ICON_COLOR_KEY = "icon_color"
