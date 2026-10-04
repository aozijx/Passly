package com.aozijx.passly.feature.backup.internal.archive

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
import com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceKind

/** Stable protocol keys. Kotlin enum names are deliberately not part of the archive format. */
internal object BackupArchiveKeyRegistry {
    private val entryTypes = mapOf(
        "account" to EntryType.ACCOUNT,
        "login" to EntryType.LOGIN,
        "note" to EntryType.NOTE,
        "bank_card" to EntryType.BANK_CARD,
        "id_card" to EntryType.ID_CARD,
        "passport" to EntryType.PASSPORT,
        "driver_license" to EntryType.DRIVER_LICENSE,
        "ssh_key" to EntryType.SSH_KEY,
        "wifi" to EntryType.WIFI,
        "passkey" to EntryType.PASSKEY,
        "otp" to EntryType.OTP,
        "database_credential" to EntryType.DATABASE_CREDENTIAL,
        "server_credential" to EntryType.SERVER_CREDENTIAL,
        "api_key" to EntryType.API_KEY,
        "crypto_wallet" to EntryType.CRYPTO_WALLET,
        "seed_phrase" to EntryType.SEED_PHRASE,
        "recovery_code" to EntryType.RECOVERY_CODE,
    )
    private val entryTypeKeys = entryTypes.entries.associate { (key, value) -> value to key }
    val archiveEntryTypes: Set<EntryType> = entryTypeKeys.keys

    private val fields = mapOf(
        "title" to FieldKey.TITLE,
        "username" to FieldKey.USERNAME,
        "email" to FieldKey.EMAIL,
        "password" to FieldKey.PASSWORD,
        "notes" to FieldKey.NOTES,
        "tags" to FieldKey.TAGS,
        "primary_url" to FieldKey.PRIMARY_URL,
        "domains" to FieldKey.DOMAINS,
        "application_ids" to FieldKey.APPLICATION_IDS,
        "expires_at" to FieldKey.EXPIRES_AT,
        "favorite" to FieldKey.FAVORITE,
        "custom_fields" to FieldKey.CUSTOM_FIELDS,
        "otp_type" to FieldKey.OTP_TYPE,
        "otp_secret" to FieldKey.OTP_SECRET,
        "otp_issuer" to FieldKey.OTP_ISSUER,
        "otp_account_name" to FieldKey.OTP_ACCOUNT_NAME,
        "otp_period" to FieldKey.OTP_PERIOD,
        "otp_counter" to FieldKey.OTP_COUNTER,
        "otp_digits" to FieldKey.OTP_DIGITS,
        "otp_algorithm" to FieldKey.OTP_ALGORITHM,
        "otp_encoding" to FieldKey.OTP_ENCODING,
        "passkey_credential_id" to FieldKey.PASSKEY_CREDENTIAL_ID,
        "passkey_relying_party_id" to FieldKey.PASSKEY_RELYING_PARTY_ID,
        "passkey_user_handle" to FieldKey.PASSKEY_USER_HANDLE,
        "passkey_data" to FieldKey.PASSKEY_DATA,
        "recovery_codes" to FieldKey.RECOVERY_CODES,
        "hardware_info" to FieldKey.HARDWARE_INFO,
        "ssh_key" to FieldKey.SSH_KEY,
        "ssh_public_key" to FieldKey.SSH_PUBLIC_KEY,
        "ssh_passphrase" to FieldKey.SSH_PASSPHRASE,
        "seed_phrase" to FieldKey.SEED_PHRASE,
        "card_type" to FieldKey.CARD_TYPE,
        "card_number" to FieldKey.CARD_NUMBER,
        "card_expiration" to FieldKey.CARD_EXPIRATION,
        "card_cvv" to FieldKey.CARD_CVV,
        "card_holder" to FieldKey.CARD_HOLDER,
        "payment_pin" to FieldKey.PAYMENT_PIN,
        "payment_platform" to FieldKey.PAYMENT_PLATFORM,
        "billing_address" to FieldKey.BILLING_ADDRESS,
        "security_question" to FieldKey.SECURITY_QUESTION,
        "security_answer" to FieldKey.SECURITY_ANSWER,
        "id_number" to FieldKey.ID_NUMBER,
        "wifi_ssid" to FieldKey.WIFI_SSID,
        "wifi_security" to FieldKey.WIFI_SECURITY,
        "wifi_hidden" to FieldKey.WIFI_HIDDEN,
    )
    private val fieldKeys = fields.entries.associate { (key, value) -> value to key }

    private val relations = mapOf(
        "member_of_account" to EntryRelationType.MEMBER_OF_ACCOUNT,
        "otp_for" to EntryRelationType.OTP_FOR,
        "recovery_for" to EntryRelationType.RECOVERY_FOR,
        "related_to" to EntryRelationType.RELATED_TO,
    )
    private val relationKeys = relations.entries.associate { (key, value) -> value to key }

    private val resources = mapOf(
        "icon" to BackupResourceKind.ICON,
        "attachment" to BackupResourceKind.ATTACHMENT,
    )
    private val resourceKeys = resources.entries.associate { (key, value) -> value to key }

    fun entryTypeKey(type: EntryType): String = entryTypeKeys.getValue(type)
    fun entryType(key: String): EntryType? = entryTypes[key]

    fun fieldKey(field: FieldKey): String = fieldKeys.getValue(field)
    fun field(key: String): FieldKey? = fields[key]

    fun relationTypeKey(type: EntryRelationType): String = relationKeys.getValue(type)
    fun relationType(key: String): EntryRelationType? = relations[key]

    fun resourceKindKey(kind: BackupResourceKind): String = resourceKeys.getValue(kind)
    fun resourceKind(key: String): BackupResourceKind? = resources[key]
}
