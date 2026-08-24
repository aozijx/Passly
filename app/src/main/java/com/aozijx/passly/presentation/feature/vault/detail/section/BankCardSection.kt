package com.aozijx.passly.presentation.feature.vault.detail.section

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.platform.ClipboardUtils
import com.aozijx.passly.core.ui.components.MaskStyle
import com.aozijx.passly.core.ui.components.PasslyOutlinedTextField
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.presentation.feature.vault.detail.DetailAuthenticate
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.RevealedFieldKey
import com.aozijx.passly.presentation.feature.vault.detail.DetailSectionActionHandler
import com.aozijx.passly.presentation.feature.vault.detail.EntryEditState
import com.aozijx.passly.presentation.feature.vault.detail.copySensitiveField
import com.aozijx.passly.presentation.feature.vault.detail.withCardCvv
import com.aozijx.passly.presentation.feature.vault.detail.withCardNumber
import com.aozijx.passly.presentation.feature.vault.detail.withDetailUsername
import com.aozijx.passly.presentation.ui.vault.detail.component.DetailItem
import com.aozijx.passly.domain.sensitive.SensitiveValue

@Composable
fun BankCardSection(
    entry: Entry,
    editState: EntryEditState,
    revealedCardholder: String?,
    revealedCardNumber: String?,
    revealedCvv: String?,
    revealedPaymentPin: String?,
    onRevealField: (String, SensitiveValue?) -> Unit,
    onAuthenticate: DetailAuthenticate,
    onEntryUpdated: (Entry) -> Unit,
    onAction: (DetailUiAction) -> Unit
) {
    val context = LocalContext.current
    val label = stringResource(R.string.vault_fab_bank_card)
    val cardCopiedMsg = stringResource(R.string.field_copy_success_message, label)
    val actionHandler = DetailSectionActionHandler(
        onAuthenticate = onAuthenticate,
        onAction = onAction
    )
    val cardSecret = entry.secret.card
    val hasCardNumber = !cardSecret?.cardNumber.isNullOrBlank() ||
        revealedCardNumber != null
    val hasCardCvv = !cardSecret?.cardCvv.isNullOrBlank() ||
        revealedCvv != null ||
        editState.isEditingTotp
    val hasPaymentPin = !cardSecret?.paymentPin.isNullOrBlank() ||
        revealedPaymentPin != null

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (editState.isEditingUsername) {
            PasslyOutlinedTextField(
                value = editState.editedUsername,
                onValueChange = { editState.editedUsername = it },
                label = stringResource(
                    R.string.field_edit_action,
                    stringResource(R.string.cardholder)
                ),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = {
                        savePlaintext(editState.editedUsername, revealedCardholder, { editState.isEditingUsername = false }) {
                            onEntryUpdated(entry.withDetailUsername(it))
                            // Wrap in SensitiveValue for consistency
                            onRevealField(RevealedFieldKey.CARDHOLDER, com.aozijx.passly.domain.sensitive.OwnedChars.fromString(editState.editedUsername))
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
                        revealedValue = revealedCardholder?.let { com.aozijx.passly.domain.sensitive.OwnedChars.fromString(it) },
                        sourceValue = entry.username,
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
            PasslyOutlinedTextField(
                value = editState.editedPassword,
                onValueChange = { editState.editedPassword = it },
                label = stringResource(
                    R.string.field_edit_action,
                    stringResource(R.string.card_number)
                ),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = {
                        savePlaintext(editState.editedPassword, revealedCardNumber, { editState.isEditingPassword = false }) {
                            onEntryUpdated(entry.withCardNumber(it))
                            onRevealField(RevealedFieldKey.CARD_NUMBER, com.aozijx.passly.domain.sensitive.OwnedChars.fromString(editState.editedPassword))
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
                value = revealedCardNumber,
                isRevealed = revealedCardNumber != null,
                onCopy = {
                    copySensitiveField(
                        context = context,
                        handler = actionHandler,
                        fieldName = "card number",
                        revealedValue = revealedCardNumber?.let { com.aozijx.passly.domain.sensitive.OwnedChars.fromString(it) },
                        sourceValue = null,
                        afterCopy = {
                            Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onEdit = {
                    editState.editedPassword = revealedCardNumber ?: ""
                    editState.isEditingPassword = true
                },
                onReveal = {
                    if (revealedCardNumber != null) {
                        onRevealField(RevealedFieldKey.CARD_NUMBER, null)
                    } else {
                        onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.CARD_NUMBER))
                    }
                }
            )
        }

        if (editState.isEditingTotp) {
            PasslyOutlinedTextField(
                value = editState.editedTotp,
                onValueChange = { editState.editedTotp = it },
                label = stringResource(
                    R.string.field_edit_action,
                    stringResource(R.string.card_cvv)
                ),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = {
                        savePlaintext(editState.editedTotp, revealedCvv, { editState.isEditingTotp = false }) {
                            onEntryUpdated(entry.withCardCvv(it))
                            onRevealField(RevealedFieldKey.CVV, com.aozijx.passly.domain.sensitive.OwnedChars.fromString(editState.editedTotp))
                        }
                    }) {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                singleLine = true
            )
        } else if (hasCardCvv) {
            DetailItem(
                label = stringResource(R.string.card_cvv),
                value = revealedCvv,
                isRevealed = revealedCvv != null,
                maskStyle = MaskStyle.SHORT,
                onCopy = {
                    copySensitiveField(
                        context = context,
                        handler = actionHandler,
                        fieldName = "CVV",
                        revealedValue = revealedCvv?.let { com.aozijx.passly.domain.sensitive.OwnedChars.fromString(it) },
                        sourceValue = null,
                        afterCopy = {
                            Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onEdit = {
                    editState.editedTotp = revealedCvv ?: ""; editState.isEditingTotp = true
                },
                onReveal = {
                    if (revealedCvv != null) {
                        onRevealField(RevealedFieldKey.CVV, null)
                    } else {
                        onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.CVV))
                    }
                }
            )
        }

        cardSecret?.cardExpiry?.let { expiration ->
            DetailItem(
                label = stringResource(R.string.card_expiration),
                value = expiration,
                isRevealed = true,
                onCopy = {
                    ClipboardUtils.copy(context, expiration)
                    Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show()
                    onAction(DetailUiAction.RecordAction("expiration", ActivityType.COPY_PASSWORD))
                },
                onEdit = null
            )
        }

        if (hasPaymentPin) {
            DetailItem(
                label = stringResource(R.string.payment_pin),
                value = revealedPaymentPin,
                isRevealed = revealedPaymentPin != null,
                onCopy = {
                    copySensitiveField(
                        context = context,
                        handler = actionHandler,
                        fieldName = "payment PIN",
                        revealedValue = revealedPaymentPin?.let { com.aozijx.passly.domain.sensitive.OwnedChars.fromString(it) },
                        sourceValue = null,
                        afterCopy = {
                            Toast.makeText(context, cardCopiedMsg, Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onEdit = null,
                onReveal = {
                    if (revealedPaymentPin != null) {
                        onRevealField(RevealedFieldKey.PAYMENT_PIN, null)
                    } else {
                        onAction(DetailUiAction.RevealHighSensitivityField(RevealedFieldKey.PAYMENT_PIN))
                    }
                }
            )
        }

        val canRevealMore =
            (hasCardNumber && revealedCardNumber == null) ||
                (hasCardCvv && revealedCvv == null) ||
                (hasPaymentPin && revealedPaymentPin == null)
        if (canRevealMore && !editState.isEditingPassword) {
            Button(
                onClick = {
                    val sensitiveKeys = buildSet {
                        if (hasCardNumber && revealedCardNumber == null) {
                            add(RevealedFieldKey.CARD_NUMBER)
                        }
                        if (hasCardCvv && revealedCvv == null) add(RevealedFieldKey.CVV)
                        if (hasPaymentPin && revealedPaymentPin == null) {
                            add(RevealedFieldKey.PAYMENT_PIN)
                        }
                    }
                    if (sensitiveKeys.isNotEmpty()) {
                        onAction(DetailUiAction.RevealHighSensitivityFields(sensitiveKeys))
                    }
                    if (revealedCardholder == null) {
                        onRevealField(RevealedFieldKey.CARDHOLDER, com.aozijx.passly.domain.sensitive.OwnedChars.fromNullableString(entry.username))
                        onAction(
                            DetailUiAction.RecordAction(
                                "cardholder",
                                ActivityType.VIEW
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
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
    value: String?,
    isRevealed: Boolean,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onReveal: () -> Unit
) = DetailItem(
    label = label,
    value = value,
    isRevealed = isRevealed,
    maskStyle = MaskStyle.DEFAULT,
    onCopy = onCopy,
    onEdit = onEdit,
    onReveal = onReveal
)

private fun savePlaintext(newValue: String, oldValue: String?, onClose: () -> Unit, onSuccess: (String) -> Unit) {
    if (newValue == oldValue) {
        onClose()
        return
    }
    onSuccess(newValue)
    onClose()
}
