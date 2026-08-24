package com.aozijx.passly.presentation.ui.vault.detail.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.aozijx.passly.core.ui.components.MaskedText
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailSshUiModel

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
        DetailItem(label = stringResource(R.string.ssh_fingerprint),
            value = model.fingerprint.ifEmpty { stringResource(R.string.not_set) },
            isRevealed = true, onCopy = onFingerprintCopy, onEdit = null)
        if (model.isEditingPassphrase) EditTextField(model.editedPassphrase, onPassphraseChanged,
            stringResource(R.string.field_edit_action, stringResource(R.string.passphrase))) {
            onPassphraseSaved(model.editedPassphrase) }
        else DetailItem(label = stringResource(R.string.passphrase), value = model.passphrase,
            isRevealed = model.passphraseRevealed, onCopy = onPassphraseCopy,
            onEdit = onPassphraseEditStarted, onReveal = onPassphraseReveal)
        Surface(onClick = onPrivateKeyClick, shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .3f),
            modifier = Modifier.padding(vertical = 4.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.ssh_private_key), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
                MaskedText(model.privateKey?.take(60)?.plus("..."), model.privateKeyRevealed,
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Start,
                    fontWeight = FontWeight.Normal, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        }
        model.privateKey?.let { key -> Card(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.ssh_private_key_full), color = MaterialTheme.colorScheme.primary)
                Text(key, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
        } }
        if (model.canRevealMore) Button(onClick = onRevealAll, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Visibility, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.vault_reveal_info)) }
    }
}
