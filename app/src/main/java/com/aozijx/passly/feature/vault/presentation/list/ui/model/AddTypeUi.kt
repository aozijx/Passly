package com.aozijx.passly.feature.vault.presentation.list.ui.model

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.model.AddType

@get:StringRes
val AddType.labelRes: Int
    get() =
        when (this) {
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

@get:StringRes
val AddType.descriptionRes: Int
    get() =
        when (this) {
            AddType.PASSWORD -> R.string.vault_add_type_password_description
            AddType.TOTP -> R.string.vault_add_type_totp_description
            AddType.BANK_CARD -> R.string.vault_add_type_bank_card_description
            AddType.WIFI -> R.string.vault_add_type_wifi_description
            AddType.SSH_KEY -> R.string.vault_add_type_ssh_key_description
            AddType.ID_CARD -> R.string.vault_add_type_id_card_description
            AddType.SEED_PHRASE -> R.string.vault_add_type_seed_phrase_description
            AddType.PASSKEY -> R.string.vault_add_type_passkey_description
            AddType.RECOVERY_CODE -> R.string.vault_add_type_recovery_code_description
        }

internal fun AddType.icon(): ImageVector =
    when (this) {
        AddType.PASSWORD -> Icons.Default.Key
        AddType.TOTP -> Icons.Default.Pin
        AddType.BANK_CARD -> Icons.Default.CreditCard
        AddType.WIFI -> Icons.Default.Wifi
        AddType.SSH_KEY -> Icons.Default.VpnKey
        AddType.ID_CARD -> Icons.Default.Badge
        AddType.SEED_PHRASE -> Icons.AutoMirrored.Filled.TextSnippet
        AddType.PASSKEY -> Icons.Default.Fingerprint
        AddType.RECOVERY_CODE -> Icons.Default.Restore
    }

internal object VaultAddTypePresentation {
    val fabMenuOptions = listOf(AddType.TOTP, AddType.PASSWORD)
}
