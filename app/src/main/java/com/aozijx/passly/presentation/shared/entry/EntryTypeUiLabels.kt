package com.aozijx.passly.presentation.shared.entry

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.EntryType

@get:StringRes
internal val EntryType.labelRes: Int
    get() = when (this) {
        EntryType.ACCOUNT -> R.string.entry_type_account
        EntryType.LOGIN -> R.string.entry_type_login
        EntryType.NOTE -> R.string.entry_type_note
        EntryType.BANK_CARD -> R.string.entry_type_bank_card
        EntryType.ID_CARD -> R.string.entry_type_id_card
        EntryType.PASSPORT -> R.string.entry_type_passport
        EntryType.DRIVER_LICENSE -> R.string.entry_type_driver_license
        EntryType.SSH_KEY -> R.string.entry_type_ssh_key
        EntryType.WIFI -> R.string.entry_type_wifi
        EntryType.PASSKEY -> R.string.entry_type_passkey
        EntryType.OTP -> R.string.entry_type_otp
        EntryType.DATABASE_CREDENTIAL -> R.string.entry_type_database_credential
        EntryType.SERVER_CREDENTIAL -> R.string.entry_type_server_credential
        EntryType.API_KEY -> R.string.entry_type_api_key
        EntryType.CRYPTO_WALLET -> R.string.entry_type_crypto_wallet
        EntryType.SEED_PHRASE -> R.string.entry_type_seed_phrase
        EntryType.RECOVERY_CODE -> R.string.entry_type_recovery_code
    }
