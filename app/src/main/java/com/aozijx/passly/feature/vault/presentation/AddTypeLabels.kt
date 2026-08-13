package com.aozijx.passly.feature.vault.presentation

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.model.AddType

@get:StringRes
val AddType.labelRes: Int
    get() = when (this) {
        AddType.PASSWORD -> R.string.password_label
        AddType.TOTP -> R.string.vault_add_type_totp
        AddType.BANK_CARD -> R.string.vault_fab_bank_card
        AddType.WIFI -> R.string.vault_fab_wifi
        AddType.SSH_KEY -> R.string.vault_fab_ssh_key
        AddType.ID_CARD -> R.string.vault_fab_id_card
        AddType.SEED_PHRASE -> R.string.seed_phrase
        AddType.PASSKEY -> R.string.vault_fab_passkey
        AddType.RECOVERY_CODE -> R.string.recovery_code_label
    }
