package com.aozijx.passly.presentation.ui.vault.detail.component

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
import com.aozijx.passly.core.ui.components.PasslyOutlinedTextField
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailBankCardUiModel

enum class DetailBankCardFieldUiModel { CARDHOLDER, CARD_NUMBER, CVV, EXPIRATION, PAYMENT_PIN }

@Composable
fun BankCardSection(
    model: DetailBankCardUiModel,
    onEditChanged: (DetailBankCardFieldUiModel, String) -> Unit,
    onEditStarted: (DetailBankCardFieldUiModel, String) -> Unit,
    onEditSaved: (DetailBankCardFieldUiModel, String) -> Unit,
    onCopy: (DetailBankCardFieldUiModel) -> Unit,
    onReveal: (DetailBankCardFieldUiModel) -> Unit,
    onRevealAll: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CardField(DetailBankCardFieldUiModel.CARDHOLDER, stringResource(R.string.cardholder), model.cardholder,
            model.cardholderRevealed, model.editingCardholder, model.editedCardholder,
            onEditChanged, onEditStarted, onEditSaved, onCopy, null)
        if (model.hasCardNumber) CardField(DetailBankCardFieldUiModel.CARD_NUMBER,
            stringResource(R.string.card_number), model.cardNumber, model.cardNumberRevealed,
            model.editingCardNumber, model.editedCardNumber, onEditChanged, onEditStarted,
            onEditSaved, onCopy, onReveal)
        if (model.hasCvv) CardField(DetailBankCardFieldUiModel.CVV, stringResource(R.string.card_cvv),
            model.cvv, model.cvvRevealed, model.editingCvv, model.editedCvv, onEditChanged,
            onEditStarted, onEditSaved, onCopy, onReveal, MaskStyle.SHORT)
        model.expiration?.let {
            DetailItem(label = stringResource(R.string.card_expiration), value = it, isRevealed = true,
                onCopy = { onCopy(DetailBankCardFieldUiModel.EXPIRATION) }, onEdit = null)
        }
        if (model.hasPaymentPin) DetailItem(
            label = stringResource(R.string.payment_pin), value = model.paymentPin,
            isRevealed = model.paymentPinRevealed,
            onCopy = { onCopy(DetailBankCardFieldUiModel.PAYMENT_PIN) }, onEdit = null,
            onReveal = { onReveal(DetailBankCardFieldUiModel.PAYMENT_PIN) })
        if (model.canRevealMore && !model.editingCardNumber) {
            Button(onClick = onRevealAll, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Visibility, null); Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.vault_reveal_info))
            }
        }
    }
}

@Composable
private fun CardField(
    field: DetailBankCardFieldUiModel, label: String, value: String?, revealed: Boolean,
    editing: Boolean, editedValue: String,
    onEditChanged: (DetailBankCardFieldUiModel, String) -> Unit,
    onEditStarted: (DetailBankCardFieldUiModel, String) -> Unit,
    onEditSaved: (DetailBankCardFieldUiModel, String) -> Unit,
    onCopy: (DetailBankCardFieldUiModel) -> Unit,
    onReveal: ((DetailBankCardFieldUiModel) -> Unit)?,
    maskStyle: MaskStyle = MaskStyle.DEFAULT,
) {
    if (editing) PasslyOutlinedTextField(
        value = editedValue, onValueChange = { onEditChanged(field, it) },
        label = stringResource(R.string.field_edit_action, label), modifier = Modifier.fillMaxWidth(),
        trailingIcon = { IconButton(onClick = { onEditSaved(field, editedValue) }) {
            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) } }, singleLine = true)
    else DetailItem(label = label, value = value, isRevealed = revealed, maskStyle = maskStyle, onCopy = { onCopy(field) },
        onEdit = { onEditStarted(field, value.orEmpty()) },
        onReveal = onReveal?.let { callback -> { callback(field) } })
}
