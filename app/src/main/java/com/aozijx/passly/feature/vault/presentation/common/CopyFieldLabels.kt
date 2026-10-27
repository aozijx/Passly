package com.aozijx.passly.feature.vault.presentation.common

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.FieldKey

/** A short copy-confirmation label; this mapping never reads a credential value. */
@StringRes
internal fun FieldKey.copyLabelRes(): Int = when (this) {
    FieldKey.PASSWORD -> R.string.password_label
    FieldKey.USERNAME -> R.string.field_username
    FieldKey.EMAIL -> R.string.copy_field_email
    FieldKey.CARD_NUMBER -> R.string.card_number
    FieldKey.CARD_CVV -> R.string.card_cvv
    FieldKey.PAYMENT_PIN -> R.string.payment_pin
    FieldKey.SSH_KEY -> R.string.ssh_private_key
    FieldKey.SEED_PHRASE -> R.string.seed_phrase
    FieldKey.ID_NUMBER -> R.string.id_number
    FieldKey.PASSKEY_DATA -> R.string.copy_field_passkey
    FieldKey.RECOVERY_CODES -> R.string.copy_field_recovery_codes
    FieldKey.NOTES -> R.string.field_notes
    else -> R.string.copy_field_content
}
