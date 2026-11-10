package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.autofill.excludeFromAutofill
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailSshFingerprintUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailSshUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.ProtectedDetailFieldUiState

@Composable
fun SshKeySection(
    model: DetailSshUiModel,
    onFingerprintCopy: () -> Unit,
    onPassphraseCopy: () -> Unit,
    onPassphraseReveal: () -> Unit,
    onPassphraseEditStarted: () -> Unit,
    onPassphraseChanged: (String) -> Unit,
    onPassphraseSaved: (String) -> Unit,
    onPrivateKeyClick: () -> Unit,
    onRevealAll: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DetailValueCard(
            title = stringResource(R.string.ssh_fingerprint),
            value =
                when (val fingerprint = model.fingerprint) {
                    DetailSshFingerprintUiState.NotSet -> stringResource(R.string.not_set)
                    is DetailSshFingerprintUiState.Available -> fingerprint.value
                },
            isRevealed = true,
            onCopy = onFingerprintCopy,
        )
        if (model.isEditingPassphrase) {
            OutlinedTextField(
                value = model.editedPassphrase,
                onValueChange = onPassphraseChanged,
                label = { Text(stringResource(R.string.field_edit_action, stringResource(R.string.passphrase))) },
                modifier = Modifier.fillMaxWidth().excludeFromAutofill(),
                trailingIcon = {
                    IconButton(onClick = { onPassphraseSaved(model.editedPassphrase) }) {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                singleLine = true,
            )
        } else {
            val passphraseValue =
                when (val passphrase = model.passphrase) {
                    ProtectedDetailFieldUiState.Absent, ProtectedDetailFieldUiState.Hidden -> ""
                    is ProtectedDetailFieldUiState.Revealed -> passphrase.value
                }
            DetailValueCard(
                title = stringResource(R.string.passphrase),
                value = passphraseValue,
                isRevealed = model.passphrase is ProtectedDetailFieldUiState.Revealed,
                onCopy = onPassphraseCopy,
                onLongPress = onPassphraseEditStarted,
                onRevealClick = onPassphraseReveal,
            )
        }
        val privateKeyPreview =
            when (val privateKey = model.privateKey) {
                ProtectedDetailFieldUiState.Absent, ProtectedDetailFieldUiState.Hidden -> ""
                is ProtectedDetailFieldUiState.Revealed -> privateKey.value.take(60).plus("...")
            }
        DetailValueCard(
            title = stringResource(R.string.ssh_private_key),
            value = privateKeyPreview,
            isRevealed = model.privateKey is ProtectedDetailFieldUiState.Revealed,
            onActivate = onPrivateKeyClick,
            onCopy = null,
        )
        when (val privateKey = model.privateKey) {
            ProtectedDetailFieldUiState.Absent, ProtectedDetailFieldUiState.Hidden -> Unit
            is ProtectedDetailFieldUiState.Revealed -> {
                Card(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.ssh_private_key_full), color = MaterialTheme.colorScheme.primary)
                        Text(privateKey.value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
        if (model.canRevealMore) {
            Button(onClick = onRevealAll, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Visibility, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.vault_reveal_info))
            }
        }
    }
}
