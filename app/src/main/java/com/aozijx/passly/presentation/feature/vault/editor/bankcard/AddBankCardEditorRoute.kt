package com.aozijx.passly.presentation.feature.vault.editor.bankcard

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.presentation.feature.vault.editor.EditorSaveEffectHandler
import com.aozijx.passly.presentation.feature.vault.editor.ui.common.rememberAddEntryFabTransitionModifier
import com.aozijx.passly.presentation.feature.vault.editor.ui.bankcard.AddBankCardEditorScreen
import com.aozijx.passly.presentation.feature.vault.editor.ui.bankcard.BankCardEditorEventHandler
import com.aozijx.passly.presentation.feature.vault.editor.ui.bankcard.BankCardEditorState
import com.aozijx.passly.presentation.feature.vault.editor.ui.bankcard.BankCardTypeUi

@Composable
fun AddBankCardEditorRoute(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: AddBankCardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val saveFailedMessage = stringResource(R.string.vault_add_bank_card_save_failed)
    val saveActionModifier = with(sharedTransitionScope) {
        rememberAddEntryFabTransitionModifier(animatedVisibilityScope)
    }
    EditorSaveEffectHandler(viewModel.effects, snackbarHostState, saveFailedMessage, onSaved)

    fun submit(action: AddBankCardAction) {
        viewModel.onAction(action)
    }
    val save = {
        keyboardController?.hide()
        submit(AddBankCardAction.Save)
    }
    val form = uiState.form
    AddBankCardEditorScreen(
        state = BankCardEditorState(
            title = form.title,
            cardType = form.cardType?.toUiType(),
            cardholder = form.cardholder,
            cardNumber = form.cardNumber,
            cardNumberError = form.cardNumberError,
            cardCvv = form.cardCvv,
            paymentPin = form.paymentPin,
            cardExpiryMonth = form.cardExpiryMonth,
            cardExpiryYear = form.cardExpiryYear,
            billingAddress = form.billingAddress,
            tags = form.tags,
            notes = form.notes,
            isCardNumberVisible = form.isCardNumberVisible,
            isCvvVisible = form.isCvvVisible,
            isPinVisible = form.isPinVisible,
            isFormValid = form.isValid,
            canSave = uiState.canSave,
            isSaving = uiState.isSaving,
        ),
        onEvent = BankCardEditorEventHandler(
            onBack = onBack,
            onSave = save,
            onTitleChange = { submit(AddBankCardAction.TitleChanged(it)) },
            onCardTypeChange = {
                submit(AddBankCardAction.CardTypeChanged(it.toFeatureType()))
            },
            onCardholderChange = {
                submit(AddBankCardAction.CardholderChanged(it))
            },
            onCardNumberChange = {
                submit(AddBankCardAction.CardNumberChanged(it))
            },
            onCardNumberVisibilityChange = {
                submit(AddBankCardAction.CardNumberVisibilityChanged(it))
            },
            onPaymentPinChange = {
                submit(AddBankCardAction.PaymentPinChanged(it))
            },
            onPinVisibilityChange = {
                submit(AddBankCardAction.PinVisibilityChanged(it))
            },
            onCvvChange = { submit(AddBankCardAction.CvvChanged(it)) },
            onCvvVisibilityChange = {
                submit(AddBankCardAction.CvvVisibilityChanged(it))
            },
            onExpiryMonthChange = {
                submit(AddBankCardAction.ExpiryMonthChanged(it))
            },
            onExpiryYearChange = {
                submit(AddBankCardAction.ExpiryYearChanged(it))
            },
            onTagsChange = { submit(AddBankCardAction.TagsChanged(it)) },
            onBillingAddressChange = {
                submit(AddBankCardAction.BillingAddressChanged(it))
            },
            onNotesChange = { submit(AddBankCardAction.NotesChanged(it)) },
        ),
        snackbarHostState = snackbarHostState,
        saveActionModifier = saveActionModifier,
    )
}

private fun CardType.toUiType() = BankCardTypeUi.valueOf(name)

private fun BankCardTypeUi.toFeatureType() = CardType.valueOf(name)
