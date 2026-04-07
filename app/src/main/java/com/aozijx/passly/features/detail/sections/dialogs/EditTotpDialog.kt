package com.aozijx.passly.features.detail.sections.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.crypto.CryptoManager
import com.aozijx.passly.core.designsystem.sections.TotpConfigForm
import com.aozijx.passly.core.designsystem.state.TotpEditState
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.features.vault.VaultViewModel

@Composable
fun EditTotpSection(
    item: VaultEntry,
    vaultViewModel: VaultViewModel,
    editState: TotpEditState,
    onEntryUpdated: (VaultEntry) -> Unit = vaultViewModel::updateVaultEntry
) {
    LaunchedEffect(editState.secret) {
        if (editState.secret.contains("Steam", ignoreCase = true)) {
            editState.applySteamPreset()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(16.dp)) {
        Text(stringResource(R.string.vault_edit_totp_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        TotpConfigForm(
            secret = editState.secret, onSecretChange = { editState.secret = it },
            period = editState.period, onPeriodChange = { editState.period = it },
            digits = editState.digits, onDigitsChange = { editState.digits = it },
            algorithm = editState.algorithm, onAlgorithmChange = { editState.algorithm = it }
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { editState.isEditing = false }) { Text(stringResource(R.string.action_cancel)) }
            Button(onClick = {
                if (editState.secret.isNotBlank()) {
                    try {
                        val encrypted = CryptoManager.encrypt(editState.secret.trim())
                        onEntryUpdated(item.copy(
                            totpSecret = encrypted,
                            totpPeriod = editState.period.toIntOrNull() ?: 30,
                            totpDigits = editState.digits.toIntOrNull() ?: 6,
                            totpAlgorithm = editState.algorithm
                        ))
                        editState.isEditing = false
                    } catch (e: Exception) {
                    }
                }
            }) { Text(stringResource(R.string.action_save)) }
        }
    }
}



