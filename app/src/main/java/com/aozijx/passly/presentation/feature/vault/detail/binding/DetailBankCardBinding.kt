package com.aozijx.passly.presentation.feature.vault.detail.binding

import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.presentation.feature.vault.detail.DetailSectionActionHandler
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiState
import com.aozijx.passly.presentation.feature.vault.detail.RevealedFieldKey
import com.aozijx.passly.presentation.ui.vault.detail.component.BankCardSection
import com.aozijx.passly.presentation.ui.vault.detail.component.DetailBankCardFieldUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailBankCardUiModel

@Composable
internal fun DetailBankCardBinding(
    entry: Entry,
    uiState: DetailUiState,
    onAction: (DetailUiAction) -> Unit,
) {
    val handler = DetailSectionActionHandler(onAction)
    val cardholder = uiState.revealed(RevealedFieldKey.CARDHOLDER)?.let { String(it.toCharArray()) }
    val cardNumber = uiState.revealed(RevealedFieldKey.CARD_NUMBER)?.let { String(it.toCharArray()) }
    val cvv = uiState.revealed(RevealedFieldKey.CVV)?.let { String(it.toCharArray()) }
    val paymentPin = uiState.revealed(RevealedFieldKey.PAYMENT_PIN)?.let { String(it.toCharArray()) }
    val card = entry.secret.card
    val hasNumber = !card?.cardNumber.isNullOrBlank() || cardNumber != null
    val hasCvv = !card?.cardCvv.isNullOrBlank() || cvv != null || uiState.fieldEdits.isEditing(RevealedFieldKey.CVV)
    val hasPin = !card?.paymentPin.isNullOrBlank() || paymentPin != null

    BankCardSection(
        model = DetailBankCardUiModel(
            cardholder ?: entry.username, cardholder != null,
            cardNumber, cardNumber != null, hasNumber,
            cvv, cvv != null, hasCvv, card?.cardExpiry,
            paymentPin, paymentPin != null, hasPin,
            uiState.fieldEdits.isEditing(RevealedFieldKey.CARDHOLDER),
            uiState.fieldEdits.draft(RevealedFieldKey.CARDHOLDER),
            uiState.fieldEdits.isEditing(RevealedFieldKey.CARD_NUMBER),
            uiState.fieldEdits.draft(RevealedFieldKey.CARD_NUMBER),
            uiState.fieldEdits.isEditing(RevealedFieldKey.CVV),
            uiState.fieldEdits.draft(RevealedFieldKey.CVV),
            (hasNumber && cardNumber == null) || (hasCvv && cvv == null) ||
                (hasPin && paymentPin == null),
        ),
        onEditChanged = { field, value ->
            field.revealedFieldKey?.let { key ->
                onAction(DetailUiAction.UpdateFieldDraft(key, value))
            }
        },
        onEditStarted = { field, value ->
            field.revealedFieldKey?.let { key ->
                onAction(DetailUiAction.StartFieldEdit(key, value))
            }
        },
        onEditSaved = { field, value ->
            field.revealedFieldKey?.let { key ->
                onAction(DetailUiAction.SaveField(key, value))
            }
        },
        onCopy = { field ->
            handler.copy(
                when (field) {
                    DetailBankCardFieldUiModel.CARDHOLDER -> FieldKey.CARD_HOLDER
                    DetailBankCardFieldUiModel.CARD_NUMBER -> FieldKey.CARD_NUMBER
                    DetailBankCardFieldUiModel.CVV -> FieldKey.CARD_CVV
                    DetailBankCardFieldUiModel.PAYMENT_PIN -> FieldKey.PAYMENT_PIN
                    DetailBankCardFieldUiModel.EXPIRATION -> FieldKey.CARD_EXPIRATION
                },
            )
        },
        onReveal = { field ->
            val key = when (field) {
                DetailBankCardFieldUiModel.CARD_NUMBER -> RevealedFieldKey.CARD_NUMBER
                DetailBankCardFieldUiModel.CVV -> RevealedFieldKey.CVV
                DetailBankCardFieldUiModel.PAYMENT_PIN -> RevealedFieldKey.PAYMENT_PIN
                else -> return@BankCardSection
            }
            onAction(DetailUiAction.ToggleFieldVisibility(key))
        },
        onRevealAll = {
            val keys = buildSet {
                if (hasNumber && cardNumber == null) add(RevealedFieldKey.CARD_NUMBER)
                if (hasCvv && cvv == null) add(RevealedFieldKey.CVV)
                if (hasPin && paymentPin == null) add(RevealedFieldKey.PAYMENT_PIN)
                if (cardholder == null) add(RevealedFieldKey.CARDHOLDER)
            }
            if (keys.isNotEmpty()) onAction(DetailUiAction.RevealFields(keys))
        },
    )
}

private val DetailBankCardFieldUiModel.revealedFieldKey: String?
    get() = when (this) {
        DetailBankCardFieldUiModel.CARDHOLDER -> RevealedFieldKey.CARDHOLDER
        DetailBankCardFieldUiModel.CARD_NUMBER -> RevealedFieldKey.CARD_NUMBER
        DetailBankCardFieldUiModel.CVV -> RevealedFieldKey.CVV
        DetailBankCardFieldUiModel.PAYMENT_PIN,
        DetailBankCardFieldUiModel.EXPIRATION,
            -> null
    }
