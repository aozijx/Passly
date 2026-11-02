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

enum class VaultAddTypeUiModel(
    internal val featureType: AddType,
    @get:StringRes internal val labelResource: Int,
    @get:StringRes internal val descriptionResource: Int,
    internal val iconProvider: () -> ImageVector,
) {
    PASSWORD(
        AddType.PASSWORD,
        R.string.password_label,
        R.string.vault_add_type_password_description,
        { Icons.Default.Key },
    ),
    TOTP(
        AddType.TOTP,
        R.string.vault_add_type_totp,
        R.string.vault_add_type_totp_description,
        { Icons.Default.Pin },
    ),
    BANK_CARD(
        AddType.BANK_CARD,
        R.string.vault_fab_bank_card,
        R.string.vault_add_type_bank_card_description,
        { Icons.Default.CreditCard },
    ),
    WIFI(
        AddType.WIFI,
        R.string.vault_fab_wifi,
        R.string.vault_add_type_wifi_description,
        { Icons.Default.Wifi },
    ),
    SSH_KEY(
        AddType.SSH_KEY,
        R.string.vault_fab_ssh_key,
        R.string.vault_add_type_ssh_key_description,
        { Icons.Default.VpnKey },
    ),
    ID_CARD(
        AddType.ID_CARD,
        R.string.vault_fab_id_card,
        R.string.vault_add_type_id_card_description,
        { Icons.Default.Badge },
    ),
    SEED_PHRASE(
        AddType.SEED_PHRASE,
        R.string.seed_phrase,
        R.string.vault_add_type_seed_phrase_description,
        { Icons.AutoMirrored.Filled.TextSnippet },
    ),
    PASSKEY(
        AddType.PASSKEY,
        R.string.vault_fab_passkey,
        R.string.vault_add_type_passkey_description,
        { Icons.Default.Fingerprint },
    ),
    RECOVERY_CODE(
        AddType.RECOVERY_CODE,
        R.string.recovery_code_label,
        R.string.vault_add_type_recovery_code_description,
        { Icons.Default.Restore },
    ),
    ;

    companion object {
        val fabMenuOptions = listOf(TOTP, PASSWORD)
    }
}

@get:StringRes
val VaultAddTypeUiModel.labelRes: Int
    get() = labelResource

@get:StringRes
val VaultAddTypeUiModel.descriptionRes: Int
    get() = descriptionResource

internal fun VaultAddTypeUiModel.icon(): ImageVector = iconProvider()
