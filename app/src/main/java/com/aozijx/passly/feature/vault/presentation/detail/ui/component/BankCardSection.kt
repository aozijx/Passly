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
        CardField(
            DetailField.CARDHOLDER,
            stringResource(R.string.cardholder),
            model.cardholder,
            true,
            model.editingCardholder,
            model.editedCardholder,
            onAction,
            false,
        )
        model.bankDetails.forEach { detail ->
            CardField(
                detail.field,
                stringResource(detail.labelRes),
                detail.value,
                true,
                detail.editing,
                detail.editedValue,
                onAction,
                false,
            )
        }
        when (val cardNumber = model.cardNumber) {
            ProtectedDetailFieldUiState.Absent -> Unit
            ProtectedDetailFieldUiState.Hidden ->
                CardField(
                    DetailField.CARD_NUMBER,
                    stringResource(R.string.card_number),
                    null,
                    false,
                    model.editingCardNumber,
                    model.editedCardNumber,
                    onAction,
                    true,
                )
            is ProtectedDetailFieldUiState.Revealed ->
                CardField(
                    DetailField.CARD_NUMBER,
                    stringResource(R.string.card_number),
                    cardNumber.value,
                    true,
                    model.editingCardNumber,
                    model.editedCardNumber,
                    onAction,
                    true,
                )
        }
        if (model.hasCvv) {
            val cvvValue =
                when (val cvv = model.cvv) {
                    ProtectedDetailFieldUiState.Absent, ProtectedDetailFieldUiState.Hidden -> null
                    is ProtectedDetailFieldUiState.Revealed -> cvv.value
                }
            CardField(
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
            DetailItem(
                label = stringResource(R.string.card_expiration),
                value = it,
                isRevealed = true,
                onCopy = { onAction(DetailUiAction.CopyField(DetailField.CARD_EXPIRATION)) },
                onEdit = null,
            )
        }
        when (val paymentPin = model.paymentPin) {
            ProtectedDetailFieldUiState.Absent -> Unit
            ProtectedDetailFieldUiState.Hidden ->
                DetailItem(
                    label = stringResource(R.string.payment_pin),
                    value = null,
                    isRevealed = false,
                    onCopy = { onAction(DetailUiAction.CopyField(DetailField.PAYMENT_PIN)) },
                    onEdit = null,
                    onReveal = { onAction(DetailUiAction.ToggleFieldVisibility(DetailField.PAYMENT_PIN)) },
                )
            is ProtectedDetailFieldUiState.Revealed ->
                DetailItem(
                    label = stringResource(R.string.payment_pin),
                    value = paymentPin.value,
                    isRevealed = true,
                    onCopy = { onAction(DetailUiAction.CopyField(DetailField.PAYMENT_PIN)) },
                    onEdit = null,
                    onReveal = { onAction(DetailUiAction.ToggleFieldVisibility(DetailField.PAYMENT_PIN)) },
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
private fun CardField(
    field: DetailField,
    label: String,
    value: String?,
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
        DetailItem(
            label = label,
            value = value,
            isRevealed = revealed,
            maskStyle = maskStyle,
            onCopy = { onAction(DetailUiAction.CopyField(field)) },
            onEdit = { onAction(DetailUiAction.StartFieldEdit(field, value.orEmpty())) },
            onReveal =
                if (canReveal) {
                    { onAction(DetailUiAction.ToggleFieldVisibility(field)) }
                } else {
                    null
                },
        )
    }
}
