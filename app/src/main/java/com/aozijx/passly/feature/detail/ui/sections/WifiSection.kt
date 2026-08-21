package com.aozijx.passly.feature.detail.ui.sections

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.core.ui.components.PasslyOutlinedTextField
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.feature.detail.DetailAuthenticate
import com.aozijx.passly.feature.detail.contract.DetailUiAction
import com.aozijx.passly.feature.detail.internal.DetailSectionActionHandler
import com.aozijx.passly.feature.detail.internal.EntryEditState
import com.aozijx.passly.feature.detail.internal.copySensitiveField
import com.aozijx.passly.feature.detail.internal.toggleRevealSensitiveField
import com.aozijx.passly.feature.detail.internal.withWifiPassword
import com.aozijx.passly.feature.detail.ui.components.DetailItem
import com.aozijx.passly.feature.detail.ui.components.InfoGroupCard
import com.aozijx.passly.domain.sensitive.OwnedChars

@Composable
fun WifiSection(
    entry: Entry,
    editState: EntryEditState,
    revealedPassword: String?,
    onPasswordRevealed: (String?) -> Unit,
    onAuthenticate: DetailAuthenticate,
    onEntryUpdated: (Entry) -> Unit,
    onAction: (DetailUiAction) -> Unit
) {
    val context = LocalContext.current
    val msgCopySuccess = stringResource(R.string.field_copy_success_message)
    val wifiSsidLabel = stringResource(R.string.wifi_ssid)
    val wifiPasswordLabel = stringResource(R.string.wifi_password)
    val wifiEncryptionLabel = stringResource(R.string.wifi_encryption)
    val wifiHiddenLabel = stringResource(R.string.wifi_hidden)
    val actionHandler = DetailSectionActionHandler(
        onAuthenticate = onAuthenticate,
        onAction = onAction
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DetailItem(
            label = wifiSsidLabel,
            value = entry.username,
            isRevealed = true,
            onCopy = {
                ClipboardUtils.copy(context, entry.username)
                onAction(DetailUiAction.RecordAction("SSID", ActivityType.COPY_PASSWORD))
            },
            onEdit = {}
        )

        if (editState.isEditingPassword) {
            PasslyOutlinedTextField(
                value = editState.editedPassword,
                onValueChange = { editState.editedPassword = it },
                label = stringResource(R.string.field_edit_action, wifiPasswordLabel),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = {
                        if (editState.editedPassword != revealedPassword) {
                            onEntryUpdated(entry.withWifiPassword(editState.editedPassword))
                            onPasswordRevealed(editState.editedPassword)
                        }
                        editState.isEditingPassword = false
                    }) {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                singleLine = true
            )
        } else {
            DetailItem(
                label = wifiPasswordLabel,
                value = revealedPassword,
                isRevealed = revealedPassword != null,
                onCopy = {
                    copySensitiveField(
                        context = context,
                        handler = actionHandler,
                        fieldName = "wifi password",
                        revealedValue = revealedPassword?.let { OwnedChars.fromString(it) },
                        sourceValue = entry.secret.wifi?.password,
                        afterCopy = {
                            Toast.makeText(
                                context,
                                msgCopySuccess.format(wifiPasswordLabel),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                },
                onEdit = {
                    editState.editedPassword = revealedPassword ?: ""
                    editState.isEditingPassword = true
                }
            )
        }

        if (revealedPassword == null && !editState.isEditingPassword) {
            Button(
                onClick = {
                    toggleRevealSensitiveField(
                        handler = actionHandler,
                        fieldName = "wifi password",
                        revealedValue = revealedPassword?.let { OwnedChars.fromString(it) },
                        sourceValue = entry.secret.wifi?.password,
                        onReveal = onPasswordRevealed
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Visibility, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.vault_reveal_info))
            }
        }

        InfoGroupCard(title = stringResource(R.string.wifi_settings)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(wifiEncryptionLabel, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        entry.secret.wifi?.securityType ?: "WPA",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(wifiHiddenLabel, style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = entry.secret.wifi?.isHidden ?: false,
                        onCheckedChange = null,
                        enabled = false
                    )
                }
            }
        }
    }
}
