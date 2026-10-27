package com.aozijx.passly.feature.vault.presentation.common

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.FieldKey

@StringRes
internal fun FieldKey.labelRes(): Int = when (this) {
    FieldKey.TITLE -> R.string.field_title
    FieldKey.USERNAME -> R.string.field_username_hint
    FieldKey.EMAIL -> R.string.revision_label_email
    FieldKey.PASSWORD -> R.string.password_label
    FieldKey.NOTES -> R.string.field_notes
    FieldKey.TAGS -> R.string.vault_detail_tags_title
    FieldKey.PRIMARY_URL -> R.string.revision_field_primary_url
    FieldKey.DOMAINS -> R.string.revision_field_domains
    FieldKey.APPLICATION_IDS -> R.string.revision_field_applications
    FieldKey.EXPIRES_AT -> R.string.revision_field_expiration
    FieldKey.FAVORITE -> R.string.revision_field_favorite
    FieldKey.CUSTOM_FIELDS -> R.string.revision_field_custom_fields
    FieldKey.OTP_TYPE -> R.string.otp_type
    FieldKey.OTP_SECRET -> R.string.revision_label_otp_secret
    FieldKey.OTP_ISSUER -> R.string.otp_issuer
    FieldKey.OTP_ACCOUNT_NAME -> R.string.otp_account_name
    FieldKey.OTP_PERIOD -> R.string.totp_period
    FieldKey.OTP_COUNTER -> R.string.otp_counter
    FieldKey.OTP_DIGITS -> R.string.revision_label_otp_digits
    FieldKey.OTP_ALGORITHM -> R.string.revision_label_otp_algorithm
    FieldKey.OTP_ENCODING -> R.string.otp_secret_encoding
    FieldKey.PASSKEY_CREDENTIAL_ID -> R.string.revision_label_passkey_credential_id
    FieldKey.PASSKEY_RELYING_PARTY_ID -> R.string.revision_label_passkey_relying_party_id
    FieldKey.PASSKEY_USER_HANDLE -> R.string.revision_label_passkey_user_handle
    FieldKey.PASSKEY_DATA -> R.string.passkey_data
    FieldKey.RECOVERY_CODES -> R.string.recovery_code_label
    FieldKey.HARDWARE_INFO -> R.string.hardware_key_info
    FieldKey.SSH_KEY -> R.string.ssh_private_key
    FieldKey.SSH_PUBLIC_KEY -> R.string.revision_label_ssh_public_key
    FieldKey.SSH_PASSPHRASE -> R.string.passphrase
    FieldKey.SEED_PHRASE -> R.string.seed_phrase
    FieldKey.CARD_TYPE -> R.string.card_type
    FieldKey.CARD_NUMBER -> R.string.card_number
    FieldKey.CARD_EXPIRATION -> R.string.card_expiration
    FieldKey.CARD_CVV -> R.string.card_cvv
    FieldKey.CARD_HOLDER -> R.string.cardholder
    FieldKey.CARD_ISSUING_BANK -> R.string.card_issuing_bank
    FieldKey.CARD_BANK_BRANCH -> R.string.card_bank_branch
    FieldKey.CARD_PRODUCT -> R.string.card_product
    FieldKey.CARD_CURRENCY -> R.string.card_currency
    FieldKey.CARD_BANK_PHONE -> R.string.card_bank_phone
    FieldKey.PAYMENT_PIN -> R.string.payment_pin
    FieldKey.PAYMENT_PLATFORM -> R.string.revision_label_payment_platform
    FieldKey.BILLING_ADDRESS -> R.string.billing_address
    FieldKey.SECURITY_QUESTION -> R.string.revision_label_security_question
    FieldKey.SECURITY_ANSWER -> R.string.revision_label_security_answer
    FieldKey.ID_NUMBER -> R.string.id_number
    FieldKey.WIFI_SSID -> R.string.wifi_ssid
    FieldKey.WIFI_SECURITY -> R.string.revision_label_wifi_security
    FieldKey.WIFI_HIDDEN -> R.string.wifi_hidden
}
