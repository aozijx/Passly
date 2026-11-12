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
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailSshPassphraseUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailSshPrivateKeyUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailSshUiModel

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
        when (val passphrase = model.passphrase) {
            is DetailSshPassphraseUiState.Editing -> {
                OutlinedTextField(
                    value = passphrase.draft,
                    onValueChange = onPassphraseChanged,
                    label = { Text(stringResource(R.string.field_edit_action, stringResource(R.string.passphrase))) },
                    modifier = Modifier.fillMaxWidth().excludeFromAutofill(),
                    trailingIcon = {
                        IconButton(onClick = { onPassphraseSaved(passphrase.draft) }) {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    singleLine = true,
                )
            }

            DetailSshPassphraseUiState.Absent,
            DetailSshPassphraseUiState.Hidden,
            -> {
                DetailValueCard(
                    title = stringResource(R.string.passphrase),
                    value = "",
                    isRevealed = false,
                    onCopy = onPassphraseCopy,
                    onRevealClick = onPassphraseReveal,
                )
            }

            is DetailSshPassphraseUiState.Revealed -> {
                DetailValueCard(
                    title = stringResource(R.string.passphrase),
                    value = passphrase.value,
                    isRevealed = true,
                    onCopy = onPassphraseCopy,
                    onLongPress = onPassphraseEditStarted,
                    onRevealClick = onPassphraseReveal,
                )
            }
        }
        when (val privateKey = model.privateKey) {
            DetailSshPrivateKeyUiState.Absent,
            DetailSshPrivateKeyUiState.Hidden,
            -> {
                DetailValueCard(
                    title = stringResource(R.string.ssh_private_key),
                    value = "",
                    isRevealed = false,
                    onActivate = onPrivateKeyClick,
                    onCopy = null,
                )
            }

            is DetailSshPrivateKeyUiState.Revealed -> {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    DetailValueCard(
                        title = stringResource(R.string.ssh_private_key),
                        value = privateKey.preview,
                        isRevealed = true,
                        onActivate = onPrivateKeyClick,
                        onCopy = null,
                    )
                    Card(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                        Column(Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.ssh_private_key_full), color = MaterialTheme.colorScheme.primary)
                            Text(privateKey.value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        }
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
