package com.aozijx.passly.feature.detail.sections

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aozijx.passly.R
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.domain.model.activity.ActivityType
import com.aozijx.passly.domain.model.entry.EntrySecret
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.feature.detail.DetailAuthenticate
import com.aozijx.passly.feature.detail.components.DetailItem
import com.aozijx.passly.feature.detail.contract.DetailIntent
import com.aozijx.passly.feature.detail.contract.RevealedFieldKey
import com.aozijx.passly.feature.detail.internal.DetailSectionActionHandler
import com.aozijx.passly.feature.detail.internal.EntryEditState
import com.aozijx.passly.feature.detail.internal.copySensitiveField

@Composable
fun BankCardSection(
    entry: VaultEntry,
    editState: EntryEditState,
    revealedCardholder: String?,
    revealedCardNumber: String?,
    revealedCvv: String?,
    revealedPaymentPin: String?,
    onRevealField: (String, String?) -> Unit,
    onAuthenticate: DetailAuthenticate,
    onEntryUpdated: (VaultEntry) -> Unit,
    onEvent: (DetailIntent) -> Unit
) {
    val context = LocalContext.current
    val cardCopiedMsg = stringResource(R.string.card_copied)
    val actionHandler = DetailSectionActionHandler(
        onAuthenticate = onAuthenticate,
        onEvent = onEvent
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (editState.isEditingUsername) {
            OutlinedTextField(
                value = editState.editedUsername,
                onValueChange = { editState.editedUsername = it },
                label = {
                    Text(
                        stringResource(
                            R.string.edit_field,
                            stringResource(R.string.cardholder)
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        savePlaintext(editState.editedUsername, revealedCardholder, { editState.isEditingUsername = false }) {
                            onEntryUpdated(entry.copy(summary = entry.summary.copy(username = it)))
                            onRevealField(RevealedFieldKey.CARDHOLDER, editState.editedUsername)
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
                    copySensitiveField(
                        context = context,
                        handler = actionHandler,
                        fieldName = "cardholder",
                        revealedValue = revealedCardholder,
                        sourceValue = entry.username,
                        authTitle = "解密持卡人",
                        authSubtitle = "验证身份以复制信息",
                        onReveal = { onRevealField(RevealedFieldKey.CARDHOLDER, it) },
                        afterCopy = {
                            Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onEdit = {
                    editState.editedUsername =
                        revealedCardholder ?: ""; editState.isEditingUsername = true
                }
            )
        }

        if (editState.isEditingPassword) {
            OutlinedTextField(
                value = editState.editedPassword,
                onValueChange = { editState.editedPassword = it },
                label = {
                    Text(
                        stringResource(
                            R.string.edit_field,
                            stringResource(R.string.card_number)
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        savePlaintext(editState.editedPassword, revealedCardNumber, { editState.isEditingPassword = false }) {
                            onEntryUpdated(
                                entry.copy(
                                    secret = (entry.secret as EntrySecret.Card).copy(
                                        data = entry.secret.data.copy(
                                            cardNumber = it
                                        )
                                    )
                                )
                            )
                            onRevealField(RevealedFieldKey.CARD_NUMBER, editState.editedPassword)
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
                value = revealedCardNumber ?: stringResource(R.string.hidden_mask),
                isRevealed = revealedCardNumber != null,
                onCopy = {
                    copySensitiveField(
                        context = context,
                        handler = actionHandler,
                        fieldName = "card number",
                        revealedValue = revealedCardNumber,
                        sourceValue = (entry.secret as? EntrySecret.Card)?.data?.cardNumber,
                        authTitle = "解密卡号",
                        authSubtitle = "验证身份以复制信息",
                        onReveal = { onRevealField(RevealedFieldKey.CARD_NUMBER, it) },
                        afterCopy = {
                            Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onEdit = { editState.editedPassword = revealedCardNumber ?: ""; editState.isEditingPassword = true }
            )
        }

        (entry.secret as? EntrySecret.Card)?.data?.cardCvv?.let { cvv ->
            if (editState.isEditingTotp) {
                OutlinedTextField(
                    value = editState.editedTotp,
                    onValueChange = { editState.editedTotp = it },
                    label = {
                        Text(
                            stringResource(
                                R.string.edit_field,
                                stringResource(R.string.card_cvv)
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = {
                            savePlaintext(editState.editedTotp, revealedCvv, { editState.isEditingTotp = false }) {
                                onEntryUpdated(
                                    entry.copy(
                                        secret = EntrySecret.Card(
                                            entry.secret.data.copy(
                                                cardCvv = it
                                            )
                                        )
                                    )
                                )
                                onRevealField(RevealedFieldKey.CVV, editState.editedTotp)
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
                    value = revealedCvv ?: stringResource(R.string.hidden_mask),
                    isRevealed = revealedCvv != null,
                    onCopy = {
                        copySensitiveField(
                            context = context,
                            handler = actionHandler,
                            fieldName = "CVV",
                            revealedValue = revealedCvv,
                            sourceValue = cvv,
                            authTitle = "解密 CVV",
                            authSubtitle = "验证身份以复制信息",
                            onReveal = { onRevealField(RevealedFieldKey.CVV, it) },
                            afterCopy = {
                                Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    onEdit = {
                        editState.editedTotp = revealedCvv ?: ""; editState.isEditingTotp = true
                    }
                )
            }
        }

        (entry.secret as? EntrySecret.Card)?.data?.cardExpiry?.let { expiration ->
            DetailItem(
                label = stringResource(R.string.card_expiration),
                value = expiration,
                isRevealed = true,
                onCopy = {
                    ClipboardUtils.copy(context, expiration)
                    Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show()
                    onEvent(DetailIntent.RecordAction("expiration", ActivityType.COPY_PASSWORD))
                },
                onEdit = {}
            )
        }

        (entry.secret as? EntrySecret.Card)?.data?.paymentPin?.let { pin ->
            DetailItem(
                label = stringResource(R.string.payment_pin),
                value = revealedPaymentPin ?: stringResource(R.string.hidden_mask),
                isRevealed = revealedPaymentPin != null,
                onCopy = {
                    copySensitiveField(
                        context = context,
                        handler = actionHandler,
                        fieldName = "payment PIN",
                        revealedValue = revealedPaymentPin,
                        sourceValue = pin,
                        authTitle = "解密支付密码",
                        authSubtitle = "验证身份以复制信息",
                        onReveal = { onRevealField(RevealedFieldKey.PAYMENT_PIN, it) },
                        afterCopy = {
                            Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onEdit = {}
            )
        }

        if (revealedCardNumber == null && !editState.isEditingPassword) {
            Button(
                onClick = {
                    onAuthenticate {
                        onRevealField(
                            RevealedFieldKey.CARD_NUMBER,
                            (entry.secret as? EntrySecret.Card)?.data?.cardNumber
                        )
                        onEvent(
                            DetailIntent.RecordAction(
                                "card number",
                                ActivityType.VIEW
                            )
                        )
                        if (revealedCvv == null && (entry.secret as? EntrySecret.Card)?.data?.cardCvv != null) {
                            onRevealField(
                                RevealedFieldKey.CVV,
                                entry.secret.data.cardCvv
                            )
                            onEvent(
                                DetailIntent.RecordAction(
                                    "CVV",
                                    ActivityType.VIEW
                                )
                            )
                        }
                        if (revealedCardholder == null) {
                            onRevealField(RevealedFieldKey.CARDHOLDER, entry.username)
                            onEvent(
                                DetailIntent.RecordAction(
                                    "cardholder",
                                    ActivityType.VIEW
                                )
                            )
                        }
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (isRevealed) onEdit() else onCopy() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(70.dp))
        Text(text = value, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), letterSpacing = if (isRevealed) 0.sp else 3.sp, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.End, maxLines = 1)
        Icon(imageVector = if (isRevealed) Icons.Default.Edit else Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.padding(start = 12.dp), tint = MaterialTheme.colorScheme.outline)
    }
}

private fun savePlaintext(newValue: String, oldValue: String?, onClose: () -> Unit, onSuccess: (String) -> Unit) {
    if (newValue == oldValue) { onClose(); return }
    onSuccess(newValue)
    onClose()
}
