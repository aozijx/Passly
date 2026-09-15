package com.aozijx.passly.presentation.feature.vault.detail.component

import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.feature.vault.detail.DetailEntryPatch
import com.aozijx.passly.presentation.feature.vault.detail.DetailEditCompletion
import com.aozijx.passly.presentation.feature.vault.detail.DetailSectionActionHandler
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiAction
import com.aozijx.passly.presentation.feature.vault.detail.DetailUiState
import com.aozijx.passly.presentation.feature.vault.detail.EntryEditState
import com.aozijx.passly.presentation.feature.vault.detail.RevealedFieldKey
import com.aozijx.passly.presentation.ui.vault.detail.component.BankCardSection
import com.aozijx.passly.presentation.ui.vault.detail.component.DetailBankCardFieldUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailBankCardUiModel

@Composable
internal fun DetailBankCardHost(
    entry: Entry,
    uiState: DetailUiState,
    editState: EntryEditState,
    onAction: (DetailUiAction) -> Unit,
) {
    val handler = DetailSectionActionHandler(onAction)
    val cardholder = uiState.revealed(RevealedFieldKey.CARDHOLDER)?.let { String(it.toCharArray()) }
    val cardNumber = uiState.revealed(RevealedFieldKey.CARD_NUMBER)?.let { String(it.toCharArray()) }
    val cvv = uiState.revealed(RevealedFieldKey.CVV)?.let { String(it.toCharArray()) }
    val paymentPin = uiState.revealed(RevealedFieldKey.PAYMENT_PIN)?.let { String(it.toCharArray()) }
    val card = entry.secret.card
    val hasNumber = !card?.cardNumber.isNullOrBlank() || cardNumber != null
    val hasCvv = !card?.cardCvv.isNullOrBlank() || cvv != null || editState.isEditingTotp
    val hasPin = !card?.paymentPin.isNullOrBlank() || paymentPin != null

    BankCardSection(
        model = DetailBankCardUiModel(
            cardholder ?: entry.username, cardholder != null,
            cardNumber, cardNumber != null, hasNumber,
            cvv, cvv != null, hasCvv, card?.cardExpiry,
            paymentPin, paymentPin != null, hasPin,
            editState.isEditingUsername, editState.editedUsername,
            editState.isEditingPassword, editState.editedPassword,
            editState.isEditingTotp, editState.editedTotp,
            (hasNumber && cardNumber == null) || (hasCvv && cvv == null) ||
                (hasPin && paymentPin == null),
        ),
        onEditChanged = { field, value ->
            when (field) {
                DetailBankCardFieldUiModel.CARDHOLDER -> editState.editedUsername = value
                DetailBankCardFieldUiModel.CARD_NUMBER -> editState.editedPassword = value
                DetailBankCardFieldUiModel.CVV -> editState.editedTotp = value
                else -> Unit
            }
        },
        onEditStarted = { field, value ->
            when (field) {
                DetailBankCardFieldUiModel.CARDHOLDER -> {
                    editState.editedUsername = value
                    editState.isEditingUsername = true
                }

                DetailBankCardFieldUiModel.CARD_NUMBER -> {
                    editState.editedPassword = value
                    editState.isEditingPassword = true
                }

                DetailBankCardFieldUiModel.CVV -> {
                    editState.editedTotp = value
                    editState.isEditingTotp = true
                }

                else -> Unit
            }
        },
        onEditSaved = { field, value ->
            when (field) {
                DetailBankCardFieldUiModel.CARDHOLDER -> onAction(
                    DetailUiAction.CommitPatch(
                        DetailEntryPatch.Username(value),
                        DetailEditCompletion.SensitiveField(RevealedFieldKey.CARDHOLDER),
                    ),
                )

                DetailBankCardFieldUiModel.CARD_NUMBER -> onAction(
                    DetailUiAction.CommitPatch(
                        DetailEntryPatch.CardNumber(value),
                        DetailEditCompletion.SensitiveField(RevealedFieldKey.CARD_NUMBER),
                    ),
                )

                DetailBankCardFieldUiModel.CVV -> onAction(
                    DetailUiAction.CommitPatch(
                        DetailEntryPatch.CardCvv(value),
                        DetailEditCompletion.SensitiveField(RevealedFieldKey.CVV),
                    ),
                )

                else -> Unit
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
            if (uiState.revealed(key) != null) {
                onAction(DetailUiAction.RevealField(key, null))
            } else {
                onAction(DetailUiAction.RevealHighSensitivityField(key))
            }
        },
        onRevealAll = {
            val keys = buildSet {
                if (hasNumber && cardNumber == null) add(RevealedFieldKey.CARD_NUMBER)
                if (hasCvv && cvv == null) add(RevealedFieldKey.CVV)
                if (hasPin && paymentPin == null) add(RevealedFieldKey.PAYMENT_PIN)
            }
            if (keys.isNotEmpty()) onAction(DetailUiAction.RevealHighSensitivityFields(keys))
            if (cardholder == null) {
                onAction(
                    DetailUiAction.RevealField(
                        RevealedFieldKey.CARDHOLDER,
                        OwnedChars.fromNullableString(entry.username),
                    ),
                )
            }
        },
    )
}
