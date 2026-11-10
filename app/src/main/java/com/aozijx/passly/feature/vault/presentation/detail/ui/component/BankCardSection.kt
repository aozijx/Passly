package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.autofill.excludeFromAutofill
import com.aozijx.passly.core.ui.components.MaskStyle
import com.aozijx.passly.feature.vault.presentation.detail.DetailUiAction
import com.aozijx.passly.feature.vault.presentation.detail.DetailField
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailBankCardUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.ProtectedDetailFieldUiState

@Composable
fun BankCardSection(
    model: DetailBankCardUiModel,
    onAction: (DetailUiAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BankCardField(
            DetailField.CARDHOLDER,
            stringResource(R.string.cardholder),
            model.cardholder.orEmpty(),
            true,
            model.editingCardholder,
            model.editedCardholder,
            onAction,
            false,
        )
        model.bankDetails.forEach { detail ->
            BankCardField(
                detail.field,
                stringResource(detail.labelRes),
                detail.value.orEmpty(),
                true,
                detail.editing,
                detail.editedValue,
                onAction,
                false,
            )
        }
        model.cardNumber.takeUnless { it is ProtectedDetailFieldUiState.Absent }?.let { cardNumber ->
            BankCardField(
                field = DetailField.CARD_NUMBER,
                label = stringResource(R.string.card_number),
                value =
                    when (cardNumber) {
                        ProtectedDetailFieldUiState.Absent, ProtectedDetailFieldUiState.Hidden -> ""
                        is ProtectedDetailFieldUiState.Revealed -> cardNumber.value
                    },
                revealed = cardNumber is ProtectedDetailFieldUiState.Revealed,
                editing = model.editingCardNumber,
                editedValue = model.editedCardNumber,
                onAction = onAction,
                canReveal = true,
            )
        }
        if (model.hasCvv) {
            val cvvValue =
                when (val cvv = model.cvv) {
                    ProtectedDetailFieldUiState.Absent, ProtectedDetailFieldUiState.Hidden -> ""
                    is ProtectedDetailFieldUiState.Revealed -> cvv.value
                }
            BankCardField(
                DetailField.CARD_CVV,
                stringResource(R.string.card_cvv),
                cvvValue,
                model.cvv is ProtectedDetailFieldUiState.Revealed,
                model.editingCvv,
                model.editedCvv,
                onAction,
                true,
                MaskStyle.SHORT,
            )
        }
        model.expiration?.let {
            DetailValueCard(
                title = stringResource(R.string.card_expiration),
                value = it,
                isRevealed = true,
                onCopy = { onAction(DetailUiAction.CopyField(DetailField.CARD_EXPIRATION)) },
            )
        }
        model.paymentPin.takeUnless { it is ProtectedDetailFieldUiState.Absent }?.let { paymentPin ->
            DetailValueCard(
                title = stringResource(R.string.payment_pin),
                value =
                    when (paymentPin) {
                        ProtectedDetailFieldUiState.Absent, ProtectedDetailFieldUiState.Hidden -> ""
                        is ProtectedDetailFieldUiState.Revealed -> paymentPin.value
                    },
                isRevealed = paymentPin is ProtectedDetailFieldUiState.Revealed,
                onCopy = { onAction(DetailUiAction.CopyField(DetailField.PAYMENT_PIN)) },
                onRevealClick = { onAction(DetailUiAction.ToggleFieldVisibility(DetailField.PAYMENT_PIN)) },
            )
        }
        if (model.canRevealMore && !model.editingCardNumber) {
            Button(
                onClick = { onAction(DetailUiAction.RevealBankCardFields) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Visibility, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.vault_reveal_info))
            }
        }
    }
}

@Composable
private fun BankCardField(
    field: DetailField,
    label: String,
    value: String,
    revealed: Boolean,
    editing: Boolean,
    editedValue: String,
    onAction: (DetailUiAction) -> Unit,
    canReveal: Boolean,
    maskStyle: MaskStyle = MaskStyle.DEFAULT,
) {
    if (editing) {
        OutlinedTextField(
            value = editedValue,
            onValueChange = { onAction(DetailUiAction.UpdateFieldDraft(field, it)) },
            label = { Text(stringResource(R.string.field_edit_action, label)) },
            modifier = Modifier.fillMaxWidth().excludeFromAutofill(),
            trailingIcon = {
                IconButton(onClick = { onAction(DetailUiAction.SaveField(field, editedValue)) }) {
                    Icon(Icons.Default.Check, stringResource(R.string.save), tint = MaterialTheme.colorScheme.primary)
                }
            },
            singleLine = true,
        )
    } else {
        DetailValueCard(
            title = label,
            value = value,
            isRevealed = revealed,
            maskStyle = maskStyle,
            onCopy = { onAction(DetailUiAction.CopyField(field)) },
            onLongPress = { onAction(DetailUiAction.StartFieldEdit(field, value)) },
            onRevealClick =
                if (canReveal) {
                    { onAction(DetailUiAction.ToggleFieldVisibility(field)) }
                } else {
                    null
                },
        )
    }
}
