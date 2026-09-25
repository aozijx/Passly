package com.aozijx.passly.presentation.feature.vault.detail.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.MaskStyle
import androidx.compose.material3.OutlinedTextField
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailBankCardUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailFieldUiModel

@Composable
fun BankCardSection(
    model: DetailBankCardUiModel,
    onAction: (DetailUiAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CardField(DetailFieldUiModel.CARDHOLDER, stringResource(R.string.cardholder), model.cardholder,
            model.cardholderRevealed, model.editingCardholder, model.editedCardholder,
            onAction, false)
        if (model.hasCardNumber) CardField(DetailFieldUiModel.CARD_NUMBER,
            stringResource(R.string.card_number), model.cardNumber, model.cardNumberRevealed,
            model.editingCardNumber, model.editedCardNumber, onAction, true)
        if (model.hasCvv) CardField(DetailFieldUiModel.CARD_CVV, stringResource(R.string.card_cvv),
            model.cvv, model.cvvRevealed, model.editingCvv, model.editedCvv,
            onAction, true, MaskStyle.SHORT)
        model.expiration?.let {
            DetailItem(label = stringResource(R.string.card_expiration), value = it, isRevealed = true,
                onCopy = { onAction(DetailUiAction.CopyField(DetailFieldUiModel.CARD_EXPIRATION)) }, onEdit = null)
        }
        if (model.hasPaymentPin) DetailItem(
            label = stringResource(R.string.payment_pin), value = model.paymentPin,
            isRevealed = model.paymentPinRevealed,
            onCopy = { onAction(DetailUiAction.CopyField(DetailFieldUiModel.PAYMENT_PIN)) }, onEdit = null,
            onReveal = { onAction(DetailUiAction.ToggleFieldVisibility(DetailFieldUiModel.PAYMENT_PIN)) })
        if (model.canRevealMore && !model.editingCardNumber) {
            Button(
                onClick = { onAction(DetailUiAction.RevealBankCardFields) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Visibility, null); Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.vault_reveal_info))
            }
        }
    }
}

@Composable
private fun CardField(
    field: DetailFieldUiModel, label: String, value: String?, revealed: Boolean,
    editing: Boolean, editedValue: String,
    onAction: (DetailUiAction) -> Unit,
    canReveal: Boolean,
    maskStyle: MaskStyle = MaskStyle.DEFAULT,
) {
    if (editing) OutlinedTextField(
        value = editedValue, onValueChange = { onAction(DetailUiAction.UpdateFieldDraft(field, it)) },
        label = { Text(stringResource(R.string.field_edit_action, label)) }, modifier = Modifier.fillMaxWidth(),
        trailingIcon = { IconButton(onClick = { onAction(DetailUiAction.SaveField(field, editedValue)) }) {
            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) } }, singleLine = true)
    else DetailItem(label = label, value = value, isRevealed = revealed, maskStyle = maskStyle,
        onCopy = { onAction(DetailUiAction.CopyField(field)) },
        onEdit = { onAction(DetailUiAction.StartFieldEdit(field, value.orEmpty())) },
        onReveal = if (canReveal) {
            { onAction(DetailUiAction.ToggleFieldVisibility(field)) }
        } else {
            null
        })
}
