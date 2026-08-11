package com.aozijx.passly.feature.detail.ui.sections

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.aozijx.passly.core.ui.components.HiddenMask
import com.aozijx.passly.core.ui.components.PasslyOutlinedTextField
import com.aozijx.passly.domain.entry.model.EntryAggregate
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.feature.detail.DetailAuthenticate
import com.aozijx.passly.feature.detail.contract.DetailIntent
import com.aozijx.passly.feature.detail.contract.RevealedFieldKey
import com.aozijx.passly.feature.detail.internal.DetailSectionActionHandler
import com.aozijx.passly.feature.detail.internal.EntryEditState
import com.aozijx.passly.feature.detail.internal.copySensitiveField
import com.aozijx.passly.feature.detail.internal.withCardCvv
import com.aozijx.passly.feature.detail.internal.withCardNumber
import com.aozijx.passly.feature.detail.internal.withDetailUsername
import com.aozijx.passly.feature.detail.ui.components.DetailItem

@Composable
fun BankCardSection(
    entry: EntryAggregate,
    editState: EntryEditState,
    revealedCardholder: String?,
    revealedCardNumber: String?,
    revealedCvv: String?,
    revealedPaymentPin: String?,
    onRevealField: (String, String?) -> Unit,
    onAuthenticate: DetailAuthenticate,
    onEntryUpdated: (EntryAggregate) -> Unit,
    onEvent: (DetailIntent) -> Unit
) {
    val context = LocalContext.current
    val label = stringResource(R.string.vault_fab_bank_card)
    val cardCopiedMsg = stringResource(R.string.msg_copy_success, label)
    val actionHandler = DetailSectionActionHandler(
        onAuthenticate = onAuthenticate,
        onEvent = onEvent
    )
    val cardSecret = entry.secret.card
    val hasCardNumber = cardSecret?.hasCardNumber == true ||
        !cardSecret?.cardNumber.isNullOrBlank() ||
        revealedCardNumber != null
    val hasCardCvv = cardSecret?.hasCardCvv == true ||
        !cardSecret?.cardCvv.isNullOrBlank() ||
        revealedCvv != null ||
        editState.isEditingTotp
    val hasPaymentPin = cardSecret?.hasPaymentPin == true ||
        !cardSecret?.paymentPin.isNullOrBlank() ||
        revealedPaymentPin != null

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (editState.isEditingUsername) {
            PasslyOutlinedTextField(
                value = editState.editedUsername,
                onValueChange = { editState.editedUsername = it },
                label = stringResource(
                    R.string.edit_field,
                    stringResource(R.string.cardholder)
                ),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = {
                        savePlaintext(editState.editedUsername, revealedCardholder, { editState.isEditingUsername = false }) {
                            onEntryUpdated(entry.withDetailUsername(it))
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
                    R.string.edit_field,
                    stringResource(R.string.card_number)
                ),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = {
                        savePlaintext(editState.editedPassword, revealedCardNumber, { editState.isEditingPassword = false }) {
                            onEntryUpdated(entry.withCardNumber(it))
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
                value = revealedCardNumber ?: HiddenMask.DEFAULT,
                isRevealed = revealedCardNumber != null,
                onCopy = {
                    copySensitiveField(
                        context = context,
                        handler = actionHandler,
                        fieldName = "card number",
                        revealedValue = revealedCardNumber,
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
                        onEvent(DetailIntent.RevealHighSensitivityField(RevealedFieldKey.CARD_NUMBER))
                    }
                }
            )
        }

        if (editState.isEditingTotp) {
            PasslyOutlinedTextField(
                value = editState.editedTotp,
                onValueChange = { editState.editedTotp = it },
                label = stringResource(
                    R.string.edit_field,
                    stringResource(R.string.card_cvv)
                ),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = {
                        savePlaintext(editState.editedTotp, revealedCvv, { editState.isEditingTotp = false }) {
                            onEntryUpdated(entry.withCardCvv(it))
                            onRevealField(RevealedFieldKey.CVV, editState.editedTotp)
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
                value = revealedCvv ?: HiddenMask.SHORT,
                isRevealed = revealedCvv != null,
                onCopy = {
                    copySensitiveField(
                        context = context,
                        handler = actionHandler,
                        fieldName = "CVV",
                        revealedValue = revealedCvv,
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
                        onEvent(DetailIntent.RevealHighSensitivityField(RevealedFieldKey.CVV))
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
                    onEvent(DetailIntent.RecordAction("expiration", ActivityType.COPY_PASSWORD))
                },
                onEdit = null
            )
        }

        if (hasPaymentPin) {
            DetailItem(
                label = stringResource(R.string.payment_pin),
                value = revealedPaymentPin ?: HiddenMask.DEFAULT,
                isRevealed = revealedPaymentPin != null,
                onCopy = {
                    copySensitiveField(
                        context = context,
                        handler = actionHandler,
                        fieldName = "payment PIN",
                        revealedValue = revealedPaymentPin,
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
                        onEvent(DetailIntent.RevealHighSensitivityField(RevealedFieldKey.PAYMENT_PIN))
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
                        onEvent(DetailIntent.RevealHighSensitivityFields(sensitiveKeys))
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
    onEdit: () -> Unit,
    onReveal: () -> Unit
) = DetailItem(
    label = label,
    value = value,
    isRevealed = isRevealed,
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
