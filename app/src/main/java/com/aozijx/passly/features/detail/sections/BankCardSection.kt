package com.aozijx.passly.features.detail.sections

import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.R
import com.aozijx.passly.core.crypto.CryptoManager
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.features.detail.components.DetailItem
import com.aozijx.passly.features.detail.internal.EntryEditState

@Composable
fun BankCardSection(
    activity: FragmentActivity,
    entry: VaultEntry,
    editState: EntryEditState,
    onUpdateVaultEntry: (VaultEntry) -> Unit,
    onAuthenticate: (activity: FragmentActivity, title: String, subtitle: String, onSuccess: () -> Unit) -> Unit,
    onEntryUpdated: (VaultEntry) -> Unit
) {
    val context = LocalContext.current
    val cardCopiedMsg = stringResource(R.string.card_copied)

    var revealedCardNumber by remember { mutableStateOf<String?>(null) }
    var revealedCvv by remember { mutableStateOf<String?>(null) }
    var revealedCardholder by remember { mutableStateOf<String?>(null) }
    var revealedPaymentPin by remember { mutableStateOf<String?>(null) }
    var revealedSecurityAnswer by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // --- 持卡人 ---
        if (editState.isEditingUsername) {
            OutlinedTextField(
                value = editState.editedUsername,
                onValueChange = { editState.editedUsername = it },
                label = { Text(stringResource(R.string.label_edit_field, stringResource(R.string.cardholder))) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        saveEncrypted(editState.editedUsername, revealedCardholder, { editState.isEditingUsername = false }) {
                            onEntryUpdated(entry.copy(username = it))
                            revealedCardholder = editState.editedUsername
                        }
                    }) {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                singleLine = true
            )
        } else {
            DetailItem(
                label = stringResource(R.string.cardholder),
                value = revealedCardholder ?: entry.username,
                isRevealed = revealedCardholder != null,
                onCopy = {
                    revealedCardholder?.let { ClipboardUtils.copy(context, it); Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show() }
                        ?: onAuthenticate(activity, "解密持卡人", "验证身份以复制信息") {
                            runCatching { CryptoManager.decrypt(entry.username) }.onSuccess {
                                ClipboardUtils.copy(context, it); revealedCardholder = it
                                Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show()
                            }
                        }
                },
                onEdit = { editState.editedUsername = revealedCardholder ?: ""; editState.isEditingUsername = true }
            )
        }

        // --- 卡号 ---
        if (editState.isEditingPassword) {
            OutlinedTextField(
                value = editState.editedPassword,
                onValueChange = { editState.editedPassword = it },
                label = { Text(stringResource(R.string.label_edit_field, stringResource(R.string.card_number))) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        saveEncrypted(editState.editedPassword, revealedCardNumber, { editState.isEditingPassword = false }) {
                            onEntryUpdated(entry.copy(password = it))
                            revealedCardNumber = editState.editedPassword
                        }
                    }) {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                singleLine = true
            )
        } else {
            BankCardNumberItem(
                label = stringResource(R.string.card_number),
                value = revealedCardNumber ?: stringResource(R.string.label_hidden_mask),
                isRevealed = revealedCardNumber != null,
                onCopy = {
                    revealedCardNumber?.let { ClipboardUtils.copy(context, it); Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show() }
                        ?: onAuthenticate(activity, "解密卡号", "验证身份以复制信息") {
                            runCatching { CryptoManager.decrypt(entry.password) }.onSuccess {
                                ClipboardUtils.copy(context, it); revealedCardNumber = it
                                Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show()
                            }
                        }
                },
                onEdit = { editState.editedPassword = revealedCardNumber ?: ""; editState.isEditingPassword = true }
            )
        }

        // --- CVV ---
        entry.cardCvv?.let { encryptedCvv ->
            if (editState.isEditingTotp) {
                OutlinedTextField(
                    value = editState.editedTotp,
                    onValueChange = { editState.editedTotp = it },
                    label = { Text(stringResource(R.string.label_edit_field, stringResource(R.string.card_cvv))) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = {
                            saveEncrypted(editState.editedTotp, revealedCvv, { editState.isEditingTotp = false }) {
                                onEntryUpdated(entry.copy(cardCvv = it))
                                revealedCvv = editState.editedTotp
                            }
                        }) {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    singleLine = true
                )
            } else {
                DetailItem(
                    label = stringResource(R.string.card_cvv),
                    value = revealedCvv ?: stringResource(R.string.label_hidden_mask),
                    isRevealed = revealedCvv != null,
                    onCopy = {
                        revealedCvv?.let { ClipboardUtils.copy(context, it); Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show() }
                            ?: onAuthenticate(activity, "解密 CVV", "验证身份以复制信息") {
                                runCatching { CryptoManager.decrypt(encryptedCvv) }.onSuccess {
                                    ClipboardUtils.copy(context, it); revealedCvv = it
                                    Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show()
                                }
                            }
                    },
                    onEdit = { editState.editedTotp = revealedCvv ?: ""; editState.isEditingTotp = true }
                )
            }
        }

        // --- 有效期 (明文) ---
        entry.cardExpiration?.let { expiration ->
            DetailItem(
                label = stringResource(R.string.card_expiration),
                value = expiration,
                isRevealed = true,
                onCopy = { ClipboardUtils.copy(context, expiration); Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show() },
                onEdit = {}
            )
        }

        // --- 支付密码 ---
        entry.paymentPin?.let { encryptedPin ->
            DetailItem(
                label = stringResource(R.string.payment_pin),
                value = revealedPaymentPin ?: stringResource(R.string.label_hidden_mask),
                isRevealed = revealedPaymentPin != null,
                onCopy = {
                    revealedPaymentPin?.let { ClipboardUtils.copy(context, it); Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show() }
                        ?: onAuthenticate(activity, "解密支付密码", "验证身份以复制信息") {
                            runCatching { CryptoManager.decrypt(encryptedPin) }.onSuccess {
                                ClipboardUtils.copy(context, it); revealedPaymentPin = it
                                Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show()
                            }
                        }
                },
                onEdit = {}
            )
        }

        // --- 支付渠道 / 密保问题 (略，结构一致) ---
        // ... 

        // --- 整体显隐按钮 ---
        if (revealedCardNumber == null && !editState.isEditingPassword) {
            Button(
                onClick = {
                    onAuthenticate(activity, "解密卡片信息", "验证身份以查看完整信息") {
                        try {
                            if (revealedCardNumber == null) revealedCardNumber = CryptoManager.decrypt(entry.password)
                            if (revealedCvv == null && entry.cardCvv != null) revealedCvv = CryptoManager.decrypt(entry.cardCvv)
                            if (revealedCardholder == null) revealedCardholder = CryptoManager.decrypt(entry.username)
                        } catch (_: Exception) {}
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Visibility, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.vault_reveal_info))
            }
        }
    }
}

@Composable
private fun BankCardNumberItem(label: String, value: String, isRevealed: Boolean, onCopy: () -> Unit, onEdit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { if (isRevealed) onEdit() else onCopy() }.padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(70.dp))
        Text(text = value, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), letterSpacing = if (isRevealed) 0.sp else 3.sp, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.End, maxLines = 1)
        Icon(imageVector = if (isRevealed) Icons.Default.Edit else Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.padding(start = 12.dp), tint = MaterialTheme.colorScheme.outline)
    }
}

private fun saveEncrypted(newValue: String, oldValue: String?, onClose: () -> Unit, onSuccess: (String) -> Unit) {
    if (newValue == oldValue) { onClose(); return }
    runCatching { CryptoManager.encrypt(newValue) }
        .onSuccess { onSuccess(it); onClose() }
        .onFailure { onClose() }
}