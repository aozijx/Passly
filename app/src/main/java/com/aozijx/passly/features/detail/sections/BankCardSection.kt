package com.aozijx.passly.features.detail.sections

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.aozijx.passly.features.main.MainViewModel
import com.aozijx.passly.features.vault.VaultViewModel

@Composable
fun BankCardSection(
    activity: FragmentActivity,
    entry: VaultEntry,
    editState: EntryEditState,
    vaultViewModel: VaultViewModel,
    mainViewModel: MainViewModel,
    onEntryUpdated: (VaultEntry) -> Unit
) {
    val context = LocalContext.current
    val cardholderLabel = stringResource(R.string.cardholder)
    val cardNumberLabel = stringResource(R.string.card_number)
    val cardCvvLabel = stringResource(R.string.card_cvv)
    val cardExpirationLabel = stringResource(R.string.card_expiration)
    val cardCopiedMsg = stringResource(R.string.card_copied)

    var revealedCardNumber by remember { mutableStateOf<String?>(null) }
    var revealedCvv by remember { mutableStateOf<String?>(null) }
    var revealedCardholder by remember { mutableStateOf<String?>(null) }
    var revealedPaymentPin by remember { mutableStateOf<String?>(null) }
    var revealedSecurityAnswer by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (editState.isEditingUsername) {
            OutlinedTextField(
                value = editState.editedUsername,
                onValueChange = { editState.editedUsername = it },
                label = { Text(stringResource(R.string.label_edit_field, cardholderLabel)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        saveEncrypted(
                            editState.editedUsername,
                            revealedCardholder,
                            { editState.isEditingUsername = false }) { encrypted ->
                            onEntryUpdated(entry.copy(username = encrypted))
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
                label = cardholderLabel,
                value = revealedCardholder ?: entry.username,
                isRevealed = revealedCardholder != null,
                onCopy = {
                    val cardholder = revealedCardholder
                    if (cardholder != null) {
                        ClipboardUtils.copy(context, cardholder)
                        Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show()
                    } else {
                        vaultViewModel.decryptSingle(
                            activity,
                            entry.username,
                            mainViewModel::authenticate
                        ) { decrypted ->
                            decrypted?.let {
                                ClipboardUtils.copy(context, it)
                                Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show()
                                revealedCardholder = it
                            }
                        }
                    }
                },
                onEdit = {
                    editState.editedUsername = revealedCardholder ?: ""
                    editState.isEditingUsername = true
                }
            )
        }

        if (editState.isEditingPassword) {
            OutlinedTextField(
                value = editState.editedPassword,
                onValueChange = { editState.editedPassword = it },
                label = { Text(stringResource(R.string.label_edit_field, cardNumberLabel)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        saveEncrypted(
                            editState.editedPassword,
                            revealedCardNumber,
                            { editState.isEditingPassword = false }) { encrypted ->
                            onEntryUpdated(entry.copy(password = encrypted))
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
                label = cardNumberLabel,
                value = revealedCardNumber ?: stringResource(R.string.label_hidden_mask),
                isRevealed = revealedCardNumber != null,
                onCopy = {
                    val cardNumber = revealedCardNumber
                    if (cardNumber != null) {
                        ClipboardUtils.copy(context, cardNumber)
                        Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show()
                    } else {
                        vaultViewModel.decryptSingle(
                            activity,
                            entry.password,
                            mainViewModel::authenticate
                        ) { decrypted ->
                            decrypted?.let {
                                ClipboardUtils.copy(context, it)
                                Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show()
                                revealedCardNumber = it
                            }
                        }
                    }
                },
                onEdit = {
                    editState.editedPassword = revealedCardNumber ?: ""
                    editState.isEditingPassword = true
                }
            )
        }

        if (entry.cardCvv != null) {
            if (editState.isEditingTotp) {
                OutlinedTextField(
                    value = editState.editedTotp,
                    onValueChange = { editState.editedTotp = it },
                    label = { Text(stringResource(R.string.label_edit_field, cardCvvLabel)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = {
                            saveEncrypted(
                                editState.editedTotp,
                                revealedCvv,
                                { editState.isEditingTotp = false }) { encrypted ->
                                onEntryUpdated(entry.copy(totpSecret = encrypted))
                                revealedCvv = editState.editedTotp
                            }
                        }) {
                            Icon(
                                Icons.Default.Check,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    singleLine = true
                )
            } else {
                DetailItem(
                    label = cardCvvLabel,
                    value = revealedCvv ?: stringResource(R.string.label_hidden_mask),
                    isRevealed = revealedCvv != null,
                    onCopy = {
                        val cvv = revealedCvv
                        if (cvv != null) {
                            ClipboardUtils.copy(context, cvv)
                            Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show()
                        } else {
                            vaultViewModel.decryptSingle(
                                activity,
                                entry.cardCvv,
                                mainViewModel::authenticate
                            ) { decrypted ->
                                decrypted?.let {
                                    ClipboardUtils.copy(context, it)
                                    Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT)
                                        .show()
                                    revealedCvv = it
                                }
                            }
                        }
                    },
                    onEdit = {
                        editState.editedTotp = revealedCvv ?: ""
                        editState.isEditingTotp = true
                    }
                )
            }
        }

        if (entry.cardExpiration != null) {
            DetailItem(
                label = cardExpirationLabel,
                value = entry.cardExpiration,
                isRevealed = true,
                onCopy = {
                    ClipboardUtils.copy(context, entry.cardExpiration)
                    Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show()
                },
                onEdit = {}
            )
        }

        // --- 支付密码 ---
        if (entry.paymentPin != null) {
            DetailItem(
                label = stringResource(R.string.payment_pin),
                value = revealedPaymentPin ?: stringResource(R.string.label_hidden_mask),
                isRevealed = revealedPaymentPin != null,
                onCopy = {
                    val pin = revealedPaymentPin
                    if (pin != null) {
                        ClipboardUtils.copy(context, pin)
                        Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show()
                    } else {
                        vaultViewModel.decryptSingle(
                            activity,
                            entry.paymentPin,
                            mainViewModel::authenticate
                        ) { decrypted ->
                            decrypted?.let {
                                ClipboardUtils.copy(context, it)
                                Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show()
                                revealedPaymentPin = it
                            }
                        }
                    }
                },
                onEdit = {}
            )
        }

        // --- 支付渠道 ---
        if (!entry.paymentPlatform.isNullOrBlank()) {
            DetailItem(
                label = stringResource(R.string.payment_platform),
                value = entry.paymentPlatform,
                isRevealed = true,
                onCopy = {
                    ClipboardUtils.copy(context, entry.paymentPlatform)
                    Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show()
                },
                onEdit = {}
            )
        }

        // --- 密保问题与答案 ---
        if (!entry.securityQuestion.isNullOrBlank()) {
            DetailItem(
                label = stringResource(R.string.security_question),
                value = entry.securityQuestion,
                isRevealed = true,
                onCopy = {
                    ClipboardUtils.copy(context, entry.securityQuestion)
                    Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show()
                },
                onEdit = {}
            )
        }

        if (entry.securityAnswer != null) {
            DetailItem(
                label = stringResource(R.string.security_answer),
                value = revealedSecurityAnswer ?: stringResource(R.string.label_hidden_mask),
                isRevealed = revealedSecurityAnswer != null,
                onCopy = {
                    val answer = revealedSecurityAnswer
                    if (answer != null) {
                        ClipboardUtils.copy(context, answer)
                        Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show()
                    } else {
                        vaultViewModel.decryptSingle(
                            activity,
                            entry.securityAnswer,
                            mainViewModel::authenticate
                        ) { decrypted ->
                            decrypted?.let {
                                ClipboardUtils.copy(context, it)
                                Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show()
                                revealedSecurityAnswer = it
                            }
                        }
                    }
                },
                onEdit = {}
            )
        }

        if (revealedCardNumber == null && !editState.isEditingPassword) {
            Button(
                onClick = {
                    vaultViewModel.decryptSingle(
                        activity,
                        entry.password,
                        mainViewModel::authenticate
                    ) { decrypted ->
                        decrypted?.let { revealedCardNumber = it }
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
private fun BankCardNumberItem(
    label: String,
    value: String,
    isRevealed: Boolean,
    onCopy: () -> Unit,
    onEdit: () -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { if (isRevealed) onEdit() else onCopy() })
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(70.dp)
        )
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            letterSpacing = if (isRevealed) 0.sp else 3.sp,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.End,
            maxLines = 1
        )
        Icon(
            imageVector = if (isRevealed) Icons.Default.Edit else Icons.Default.ContentCopy,
            contentDescription = null,
            modifier = Modifier.padding(start = 12.dp),
            tint = MaterialTheme.colorScheme.outline
        )
    }
}

private fun saveEncrypted(
    newValue: String,
    oldValue: String?,
    onClose: () -> Unit,
    onSuccess: (String) -> Unit
) {
    if (newValue == oldValue) {
        onClose()
    } else {
        try {
            val encrypted = CryptoManager.encrypt(newValue)
            onSuccess(encrypted)
            onClose()
        } catch (e: Exception) {
            onClose()
        }
    }
}