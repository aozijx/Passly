package com.aozijx.passly.presentation.ui.vault.detail.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailEntryTypeUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.RelatedEntryUiModel

@Composable
fun RelatedEntriesSection(
    entries: List<RelatedEntryUiModel>,
    onOpenEntry: (String) -> Unit
) {
    if (entries.isEmpty()) return
    InfoGroupCard(title = stringResource(R.string.related_entries)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            entries.forEachIndexed { index, entry ->
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenEntry(entry.id) }
                        .padding(horizontal = 4.dp),
                    leadingContent = null,
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    overlineContent = null,
                    supportingContent = { Text(entry.entryType.localizedName()) },
                    colors = ListItemDefaults.colors(),
                    elevation = ListItemDefaults.elevation(ListItemDefaults.Elevation),
                    content = { Text(entry.title) },
                )
                if (index < entries.lastIndex) HorizontalDivider()
            }
        }
    }
}

@Composable
private fun DetailEntryTypeUiModel.localizedName(): String = stringResource(
    when (this) {
        DetailEntryTypeUiModel.ACCOUNT -> R.string.entry_type_account
        DetailEntryTypeUiModel.LOGIN -> R.string.entry_type_login
        DetailEntryTypeUiModel.NOTE -> R.string.entry_type_note
        DetailEntryTypeUiModel.SSH_KEY -> R.string.entry_type_ssh_key
        DetailEntryTypeUiModel.WIFI -> R.string.entry_type_wifi
        DetailEntryTypeUiModel.PASSKEY -> R.string.entry_type_passkey
        DetailEntryTypeUiModel.OTP -> R.string.entry_type_otp
        DetailEntryTypeUiModel.PASSPORT -> R.string.entry_type_passport
        DetailEntryTypeUiModel.DRIVER_LICENSE -> R.string.entry_type_driver_license
        DetailEntryTypeUiModel.DATABASE_CREDENTIAL -> R.string.entry_type_database_credential
        DetailEntryTypeUiModel.SERVER_CREDENTIAL -> R.string.entry_type_server_credential
        DetailEntryTypeUiModel.API_KEY -> R.string.entry_type_api_key
        DetailEntryTypeUiModel.CRYPTO_WALLET -> R.string.entry_type_crypto_wallet
        DetailEntryTypeUiModel.BANK_CARD -> R.string.entry_type_bank_card
        DetailEntryTypeUiModel.ID_CARD -> R.string.entry_type_id_card
        DetailEntryTypeUiModel.SEED_PHRASE -> R.string.entry_type_seed_phrase
        DetailEntryTypeUiModel.RECOVERY_CODE -> R.string.entry_type_recovery_code
    }
)
