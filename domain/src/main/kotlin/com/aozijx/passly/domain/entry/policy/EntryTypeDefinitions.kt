package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.EntryFieldAccess
import com.aozijx.passly.domain.entry.model.EntryFieldDefinition
import com.aozijx.passly.domain.entry.model.EntryFieldValueType
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryTypeDefinition
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey

/** Complete domain-owned field catalog used by create and edit flows. */
object EntryTypeDefinitions {
    private val definitions: Map<EntryType, EntryTypeDefinition> =
        EntryType.entries.associateWith(::definitionFor)

    val all: Collection<EntryTypeDefinition> get() = definitions.values

    operator fun get(type: EntryType): EntryTypeDefinition = definitions.getValue(type)

    private fun definitionFor(type: EntryType): EntryTypeDefinition = EntryTypeDefinition(
        type = type,
        fields = when (type) {
            EntryType.ACCOUNT -> accountFields()
            EntryType.NOTE -> noteFields()
            EntryType.LOGIN,
            EntryType.DATABASE_CREDENTIAL,
            EntryType.SERVER_CREDENTIAL,
            EntryType.API_KEY,
            EntryType.CRYPTO_WALLET -> loginFields()

            EntryType.BANK_CARD -> cardFields()
            EntryType.ID_CARD,
            EntryType.PASSPORT,
            EntryType.DRIVER_LICENSE -> identityFields()

            EntryType.SSH_KEY -> sshFields()
            EntryType.WIFI -> wifiFields()
            EntryType.PASSKEY -> passkeyFields()
            EntryType.OTP -> otpFields()
            EntryType.SEED_PHRASE -> seedPhraseFields()
            EntryType.RECOVERY_CODE -> recoveryCodeFields()
        },
    )

    private fun accountFields() = listOf(
        title(),
        field(FieldKey.USERNAME),
        field(FieldKey.TAGS, EntryFieldValueType.TEXT_LIST),
    )

    private fun noteFields() = listOf(
        title(),
        secret(FieldKey.NOTES),
        field(FieldKey.TAGS, EntryFieldValueType.TEXT_LIST),
    )

    private fun loginFields() = listOf(
        title(),
        secret(FieldKey.USERNAME, required = true),
        secret(FieldKey.EMAIL),
        high(FieldKey.PASSWORD, SensitiveFieldKey.PASSWORD, required = true),
        field(FieldKey.PRIMARY_URL),
        field(FieldKey.DOMAINS, EntryFieldValueType.TEXT_LIST),
        field(FieldKey.APPLICATION_IDS, EntryFieldValueType.TEXT_LIST),
        secret(FieldKey.NOTES),
        field(FieldKey.TAGS, EntryFieldValueType.TEXT_LIST),
    )

    private fun cardFields() = listOf(
        title(),
        field(FieldKey.CARD_TYPE),
        secret(FieldKey.CARD_HOLDER),
        high(FieldKey.CARD_NUMBER, SensitiveFieldKey.CARD_NUMBER, required = true),
        secret(FieldKey.CARD_EXPIRATION),
        high(FieldKey.CARD_CVV, SensitiveFieldKey.CARD_CVV),
        high(FieldKey.PAYMENT_PIN, SensitiveFieldKey.CARD_PAYMENT_PIN),
        field(FieldKey.PAYMENT_PLATFORM),
        secret(FieldKey.BILLING_ADDRESS),
        secret(FieldKey.NOTES),
        field(FieldKey.TAGS, EntryFieldValueType.TEXT_LIST),
    )

    private fun identityFields() = listOf(
        title(),
        high(FieldKey.ID_NUMBER, SensitiveFieldKey.IDENTITY_NUMBER, required = true),
        secret(FieldKey.SECURITY_QUESTION),
        secret(FieldKey.SECURITY_ANSWER),
        secret(FieldKey.NOTES),
        field(FieldKey.TAGS, EntryFieldValueType.TEXT_LIST),
    )

    private fun sshFields() = listOf(
        title(),
        secret(FieldKey.USERNAME),
        high(FieldKey.SSH_KEY, SensitiveFieldKey.SSH_PRIVATE_KEY, required = true),
        secret(FieldKey.SSH_PUBLIC_KEY),
        high(FieldKey.SSH_PASSPHRASE, SensitiveFieldKey.SSH_PASSPHRASE),
        field(FieldKey.PRIMARY_URL),
        secret(FieldKey.NOTES),
        field(FieldKey.TAGS, EntryFieldValueType.TEXT_LIST),
    )

    private fun wifiFields() = listOf(
        title(),
        field(FieldKey.WIFI_SSID, required = true),
        secret(FieldKey.PASSWORD, required = true),
        field(FieldKey.WIFI_SECURITY),
        field(FieldKey.WIFI_HIDDEN, EntryFieldValueType.BOOLEAN),
        secret(FieldKey.NOTES),
        field(FieldKey.TAGS, EntryFieldValueType.TEXT_LIST),
    )

    private fun passkeyFields() = listOf(
        title(),
        secret(FieldKey.PASSKEY_CREDENTIAL_ID),
        field(FieldKey.PASSKEY_RELYING_PARTY_ID),
        secret(FieldKey.PASSKEY_USER_HANDLE),
        high(FieldKey.PASSKEY_DATA, SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE, required = true),
        field(FieldKey.HARDWARE_INFO),
        secret(FieldKey.NOTES),
        field(FieldKey.TAGS, EntryFieldValueType.TEXT_LIST),
    )

    private fun otpFields() = listOf(
        title(),
        high(FieldKey.OTP_SECRET, SensitiveFieldKey.OTP_SECRET, required = true),
        field(FieldKey.OTP_ISSUER),
        field(FieldKey.OTP_ACCOUNT_NAME),
        field(FieldKey.OTP_TYPE),
        field(FieldKey.OTP_PERIOD, EntryFieldValueType.INTEGER),
        field(FieldKey.OTP_COUNTER, EntryFieldValueType.INTEGER),
        field(FieldKey.OTP_DIGITS, EntryFieldValueType.INTEGER),
        field(FieldKey.OTP_ALGORITHM),
        secret(FieldKey.NOTES),
        field(FieldKey.TAGS, EntryFieldValueType.TEXT_LIST),
    )

    private fun seedPhraseFields() = listOf(
        title(),
        high(FieldKey.SEED_PHRASE, SensitiveFieldKey.SEED_PHRASE, required = true),
        secret(FieldKey.NOTES),
        field(FieldKey.TAGS, EntryFieldValueType.TEXT_LIST),
    )

    private fun recoveryCodeFields() = listOf(
        title(),
        high(
            FieldKey.RECOVERY_CODES,
            SensitiveFieldKey.RECOVERY_CODES,
            valueType = EntryFieldValueType.TEXT_LIST,
            required = true,
        ),
        secret(FieldKey.NOTES),
        field(FieldKey.TAGS, EntryFieldValueType.TEXT_LIST),
    )

    private fun title() = field(FieldKey.TITLE, required = true)

    private fun field(
        key: FieldKey,
        valueType: EntryFieldValueType = EntryFieldValueType.TEXT,
        required: Boolean = false,
    ) = EntryFieldDefinition(key = key, valueType = valueType, required = required)

    private fun secret(
        key: FieldKey,
        required: Boolean = false,
    ) = EntryFieldDefinition(
        key = key,
        required = required,
        access = EntryFieldAccess.SECRET,
    )

    private fun high(
        key: FieldKey,
        sensitiveFieldKey: SensitiveFieldKey,
        valueType: EntryFieldValueType = EntryFieldValueType.TEXT,
        required: Boolean = false,
    ) = EntryFieldDefinition(
        key = key,
        valueType = valueType,
        required = required,
        access = EntryFieldAccess.HIGH_SENSITIVITY,
        sensitiveFieldKey = sensitiveFieldKey,
    )
}
