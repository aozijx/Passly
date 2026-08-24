package com.aozijx.passly.presentation.ui.vault.list.model

import androidx.annotation.StringRes
import com.aozijx.passly.R

@get:StringRes
val VaultAddTypeUiModel.labelRes: Int
    get() = when (this) {
        VaultAddTypeUiModel.PASSWORD -> R.string.password_label
        VaultAddTypeUiModel.TOTP -> R.string.vault_add_type_totp
        VaultAddTypeUiModel.BANK_CARD -> R.string.vault_fab_bank_card
        VaultAddTypeUiModel.WIFI -> R.string.vault_fab_wifi
        VaultAddTypeUiModel.SSH_KEY -> R.string.vault_fab_ssh_key
        VaultAddTypeUiModel.ID_CARD -> R.string.vault_fab_id_card
        VaultAddTypeUiModel.SEED_PHRASE -> R.string.seed_phrase
        VaultAddTypeUiModel.PASSKEY -> R.string.vault_fab_passkey
        VaultAddTypeUiModel.RECOVERY_CODE -> R.string.recovery_code_label
    }
