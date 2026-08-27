package com.aozijx.passly.presentation.ui.shared.entry

import androidx.annotation.StringRes
import com.aozijx.passly.R

@get:StringRes
internal val EntryTypeUiModel.labelRes: Int
    get() = when (this) {
        EntryTypeUiModel.ACCOUNT -> R.string.entry_type_account
        EntryTypeUiModel.LOGIN -> R.string.entry_type_login
        EntryTypeUiModel.NOTE -> R.string.entry_type_note
        EntryTypeUiModel.BANK_CARD -> R.string.entry_type_bank_card
        EntryTypeUiModel.ID_CARD -> R.string.entry_type_id_card
        EntryTypeUiModel.PASSPORT -> R.string.entry_type_passport
        EntryTypeUiModel.DRIVER_LICENSE -> R.string.entry_type_driver_license
        EntryTypeUiModel.SSH_KEY -> R.string.entry_type_ssh_key
        EntryTypeUiModel.WIFI -> R.string.entry_type_wifi
        EntryTypeUiModel.PASSKEY -> R.string.entry_type_passkey
        EntryTypeUiModel.OTP -> R.string.entry_type_otp
        EntryTypeUiModel.DATABASE_CREDENTIAL -> R.string.entry_type_database_credential
        EntryTypeUiModel.SERVER_CREDENTIAL -> R.string.entry_type_server_credential
        EntryTypeUiModel.API_KEY -> R.string.entry_type_api_key
        EntryTypeUiModel.CRYPTO_WALLET -> R.string.entry_type_crypto_wallet
        EntryTypeUiModel.SEED_PHRASE -> R.string.entry_type_seed_phrase
        EntryTypeUiModel.RECOVERY_CODE -> R.string.entry_type_recovery_code
    }
