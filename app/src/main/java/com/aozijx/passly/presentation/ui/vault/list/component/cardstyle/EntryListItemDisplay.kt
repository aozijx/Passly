package com.aozijx.passly.presentation.ui.vault.list.component.cardstyle

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.VaultItemIcon
import com.aozijx.passly.presentation.ui.vault.list.model.VaultEntryTypeUiModel
import com.aozijx.passly.presentation.ui.vault.list.model.VaultListItemUiModel

@Composable
internal fun VaultListItemUiModel.categoryOrTemplateLabel(): String =
    category ?: stringResource(entryType.labelRes)

@Composable
internal fun VaultListItemIcon(modifier: Modifier, item: VaultListItemUiModel) = VaultItemIcon(
    modifier = modifier,
    iconName = item.iconName,
    iconCustomPath = item.iconCustomPath,
    associatedAppPackage = item.associatedAppPackage,
    entryTypeKey = item.entryType.name,
    title = item.title,
    username = item.username,
    associatedDomain = item.associatedDomain,
)

private val VaultEntryTypeUiModel.labelRes: Int
    get() = when (this) {
        VaultEntryTypeUiModel.ACCOUNT -> R.string.entry_type_account
        VaultEntryTypeUiModel.LOGIN -> R.string.entry_type_login
        VaultEntryTypeUiModel.NOTE -> R.string.entry_type_note
        VaultEntryTypeUiModel.BANK_CARD -> R.string.entry_type_bank_card
        VaultEntryTypeUiModel.ID_CARD -> R.string.entry_type_id_card
        VaultEntryTypeUiModel.PASSPORT -> R.string.entry_type_passport
        VaultEntryTypeUiModel.DRIVER_LICENSE -> R.string.entry_type_driver_license
        VaultEntryTypeUiModel.SSH_KEY -> R.string.entry_type_ssh_key
        VaultEntryTypeUiModel.WIFI -> R.string.entry_type_wifi
        VaultEntryTypeUiModel.PASSKEY -> R.string.entry_type_passkey
        VaultEntryTypeUiModel.OTP -> R.string.entry_type_otp
        VaultEntryTypeUiModel.DATABASE_CREDENTIAL -> R.string.entry_type_database_credential
        VaultEntryTypeUiModel.SERVER_CREDENTIAL -> R.string.entry_type_server_credential
        VaultEntryTypeUiModel.API_KEY -> R.string.entry_type_api_key
        VaultEntryTypeUiModel.CRYPTO_WALLET -> R.string.entry_type_crypto_wallet
        VaultEntryTypeUiModel.SEED_PHRASE -> R.string.entry_type_seed_phrase
        VaultEntryTypeUiModel.RECOVERY_CODE -> R.string.entry_type_recovery_code
    }
