package com.aozijx.passly.core.ui.text

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.EntryType

@StringRes
fun EntryType.labelRes(): Int = when (this) {
    EntryType.ACCOUNT -> R.string.entry_type_account
    EntryType.LOGIN -> R.string.entry_type_login
    EntryType.NOTE -> R.string.entry_type_note
    EntryType.CARD -> R.string.entry_type_card
    EntryType.IDENTITY -> R.string.entry_type_identity
    EntryType.SSH_KEY -> R.string.entry_type_ssh_key
    EntryType.WIFI -> R.string.entry_type_wifi
    EntryType.PASSKEY -> R.string.entry_type_passkey
    EntryType.OTP -> R.string.entry_type_otp
    EntryType.PASSPORT -> R.string.entry_type_passport
    EntryType.LICENSE -> R.string.entry_type_license
    EntryType.DATABASE -> R.string.entry_type_database
    EntryType.SERVER -> R.string.entry_type_server
    EntryType.API_KEY -> R.string.entry_type_api_key
    EntryType.CRYPTO_WALLET -> R.string.entry_type_crypto_wallet
    EntryType.BANK_CARD -> R.string.entry_type_bank_card
    EntryType.ID_CARD -> R.string.entry_type_id_card
    EntryType.SEED_PHRASE -> R.string.entry_type_seed_phrase
    EntryType.RECOVERY_CODE -> R.string.entry_type_recovery_code
}

@Composable
fun EntryType.localizedName(): String = stringResource(labelRes())
