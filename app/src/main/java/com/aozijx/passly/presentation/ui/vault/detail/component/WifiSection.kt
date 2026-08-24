package com.aozijx.passly.presentation.ui.vault.detail.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.PasslyOutlinedTextField
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailWifiUiModel

@Composable
fun WifiSection(
    model: DetailWifiUiModel,
    onSsidCopy: () -> Unit,
    onPasswordCopy: () -> Unit,
    onPasswordReveal: () -> Unit,
    onPasswordEditStarted: () -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPasswordSaved: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DetailItem(label = stringResource(R.string.wifi_ssid), value = model.ssid,
            isRevealed = true, onCopy = onSsidCopy, onEdit = null)
        if (model.isEditingPassword) PasslyOutlinedTextField(
            value = model.editedPassword, onValueChange = onPasswordChanged,
            label = stringResource(R.string.field_edit_action, stringResource(R.string.wifi_password)),
            modifier = Modifier.fillMaxWidth(), trailingIcon = {
                IconButton(onClick = { onPasswordSaved(model.editedPassword) }) {
                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
            }, singleLine = true)
        else DetailItem(label = stringResource(R.string.wifi_password), value = model.password,
            isRevealed = model.passwordRevealed, onCopy = onPasswordCopy,
            onEdit = onPasswordEditStarted)
        if (!model.passwordRevealed && !model.isEditingPassword) Button(
            onClick = onPasswordReveal, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Visibility, null); Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.vault_reveal_info))
        }
        InfoGroupCard(title = stringResource(R.string.wifi_settings)) {
            Column(Modifier.padding(16.dp), Arrangement.spacedBy(12.dp)) {
                SettingRow(stringResource(R.string.wifi_encryption), model.securityType)
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(stringResource(R.string.wifi_hidden), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = model.isHidden, onCheckedChange = null, enabled = false)
                }
            }
        }
    }
}

@Composable private fun SettingRow(label: String, value: String) = Row(
    Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
    Text(label, style = MaterialTheme.typography.bodyLarge)
    Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
}
