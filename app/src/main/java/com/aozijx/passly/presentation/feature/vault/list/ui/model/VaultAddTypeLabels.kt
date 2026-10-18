package com.aozijx.passly.presentation.feature.vault.list.ui.model

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

@get:StringRes
val VaultAddTypeUiModel.descriptionRes: Int
    get() = when (this) {
        VaultAddTypeUiModel.PASSWORD -> R.string.vault_add_type_password_description
        VaultAddTypeUiModel.TOTP -> R.string.vault_add_type_totp_description
        VaultAddTypeUiModel.BANK_CARD -> R.string.vault_add_type_bank_card_description
        VaultAddTypeUiModel.WIFI -> R.string.vault_add_type_wifi_description
        VaultAddTypeUiModel.SSH_KEY -> R.string.vault_add_type_ssh_key_description
        VaultAddTypeUiModel.ID_CARD -> R.string.vault_add_type_id_card_description
        VaultAddTypeUiModel.SEED_PHRASE -> R.string.vault_add_type_seed_phrase_description
        VaultAddTypeUiModel.PASSKEY -> R.string.vault_add_type_passkey_description
        VaultAddTypeUiModel.RECOVERY_CODE -> R.string.vault_add_type_recovery_code_description
    }
