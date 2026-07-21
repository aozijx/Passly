package com.aozijx.passly.feature.detail.sections.dialogs

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
import com.aozijx.passly.domain.model.core.OtpConfig
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.feature.detail.internal.TotpEditState
import com.aozijx.passly.feature.vault.components.TotpConfigForm

@Composable
fun EditTotpSection(
    item: VaultEntry,
    editState: TotpEditState,
    onEntryUpdated: (VaultEntry) -> Unit
) {
    LaunchedEffect(editState.secret) {
        if (editState.secret.contains("Steam", ignoreCase = true)) {
            editState.applySteamPreset()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(16.dp)) {
        Text(
            stringResource(R.string.vault_edit_totp_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        TotpConfigForm(
            secret = editState.secret, onSecretChange = { editState.secret = it },
            period = editState.period, onPeriodChange = { editState.period = it },
            digits = editState.digits, onDigitsChange = { editState.digits = it },
            algorithm = editState.algorithm, onAlgorithmChange = { editState.algorithm = it }
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { editState.isEditing = false }) {
                Text(stringResource(R.string.cancel))
            }
            Button(onClick = {
                if (editState.secret.isNotBlank()) {
                    try {
                        onEntryUpdated(
                            item.copy(
                                credential = item.credential.copy(
                                    twoFactorType = item.credential.twoFactorType,
                                    otp = OtpConfig(
                                        secret = editState.secret.trim(),
                                        digits = editState.digits.toIntOrNull() ?: 6,
                                        period = editState.period.toIntOrNull() ?: 30,
                                        algorithm = editState.algorithm,
                                        issuer = item.credential.otp?.issuer,
                                        label = item.credential.otp?.label
                                    )
                                )
                            )
                        )
                        editState.isEditing = false
                    } catch (e: Exception) {
                    }
                }
            }) {
                Text(stringResource(R.string.save))
            }
        }
    }
}