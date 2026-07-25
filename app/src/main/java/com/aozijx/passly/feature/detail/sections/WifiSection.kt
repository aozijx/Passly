package com.aozijx.passly.feature.detail.sections

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.HiddenMask
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.feature.detail.DetailAuthenticate
import com.aozijx.passly.feature.detail.components.DetailItem
import com.aozijx.passly.feature.detail.components.InfoGroupCard
import com.aozijx.passly.feature.detail.contract.DetailIntent
import com.aozijx.passly.feature.detail.internal.DetailSectionActionHandler
import com.aozijx.passly.feature.detail.internal.EntryEditState
import com.aozijx.passly.feature.detail.internal.copySensitiveField
import com.aozijx.passly.feature.detail.internal.toggleRevealSensitiveField

@Composable
fun WifiSection(
    entry: VaultEntry,
    editState: EntryEditState,
    revealedPassword: String?,
    onPasswordRevealed: (String?) -> Unit,
    onAuthenticate: DetailAuthenticate,
    onEntryUpdated: (VaultEntry) -> Unit,
    onEvent: (DetailIntent) -> Unit
) {
    val context = LocalContext.current
    val wifiSsidLabel = stringResource(R.string.wifi_ssid)
    val wifiPasswordLabel = stringResource(R.string.wifi_password)
    val wifiEncryptionLabel = stringResource(R.string.wifi_encryption)
    val wifiHiddenLabel = stringResource(R.string.wifi_hidden)
    val wifiCopiedMsg = stringResource(R.string.wifi_copied)
    val actionHandler = DetailSectionActionHandler(
        onAuthenticate = onAuthenticate,
        onEvent = onEvent
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DetailItem(
            label = wifiSsidLabel,
            value = entry.username,
            isRevealed = true,
            onCopy = {
                ClipboardUtils.copy(context, entry.username)
                onEvent(DetailIntent.RecordAction("SSID", ActivityType.COPY_PASSWORD))
            },
            onEdit = {}
        )

        if (editState.isEditingPassword) {
            OutlinedTextField(
                value = editState.editedPassword,
                onValueChange = { editState.editedPassword = it },
                label = { Text(stringResource(R.string.edit_field, wifiPasswordLabel)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        if (editState.editedPassword != revealedPassword) {
                            val wifi = entry.secret.wifi
                            onEntryUpdated(
                                entry.copy(
                                    secret = entry.secret.copy(
                                        wifi = wifi?.copy(password = editState.editedPassword)
                                    )
                                )
                            )
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
                value = revealedPassword ?: HiddenMask.DEFAULT,
                isRevealed = revealedPassword != null,
                onCopy = {
                    copySensitiveField(
                        context = context,
                        handler = actionHandler,
                        fieldName = "wifi password",
                        revealedValue = revealedPassword,
                        sourceValue = entry.secret.wifi?.password,
                        authTitle = "解密 WiFi 密码",
                        authSubtitle = "验证身份以复制密码",
                        onReveal = onPasswordRevealed,
                        afterCopy = {
                            Toast.makeText(context, wifiCopiedMsg, Toast.LENGTH_SHORT).show()
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
                        revealedValue = revealedPassword,
                        sourceValue = entry.secret.wifi?.password,
                        authTitle = "解密 WiFi 密码",
                        authSubtitle = "验证身份以查看密码",
                        onReveal = onPasswordRevealed
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
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
