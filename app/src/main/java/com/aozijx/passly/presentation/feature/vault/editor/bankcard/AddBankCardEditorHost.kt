package com.aozijx.passly.presentation.feature.vault.editor.bankcard

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.presentation.ui.vault.editor.bankcard.AddBankCardEditorScreen
import com.aozijx.passly.presentation.ui.vault.editor.bankcard.BankCardEditorEventHandler
import com.aozijx.passly.presentation.ui.vault.editor.bankcard.BankCardEditorState
import com.aozijx.passly.presentation.ui.vault.editor.bankcard.BankCardTypeUi

@Composable
fun AddBankCardEditorHost(
    viewModel: AddBankCardViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onUserInteraction: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val saveFailedMessage = stringResource(R.string.vault_add_bank_card_save_failed)
    val latestOnSaved by rememberUpdatedState(onSaved)

    LaunchedEffect(viewModel, snackbarHostState, saveFailedMessage) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AddBankCardEffect.Saved -> latestOnSaved()
                is AddBankCardEffect.SaveFailed -> {
                    snackbarHostState.showSnackbar(effect.message ?: saveFailedMessage)
                }
            }
        }
    }

    fun submit(action: AddBankCardAction, userInitiated: Boolean = false) {
        if (userInitiated) onUserInteraction()
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
            onTitleChange = { submit(AddBankCardAction.TitleChanged(it), true) },
            onCardTypeChange = {
                submit(AddBankCardAction.CardTypeChanged(it.toFeatureType()), true)
            },
            onCardholderChange = {
                submit(AddBankCardAction.CardholderChanged(it), true)
            },
            onCardNumberChange = {
                submit(AddBankCardAction.CardNumberChanged(it), true)
            },
            onCardNumberVisibilityChange = {
                submit(AddBankCardAction.CardNumberVisibilityChanged(it))
            },
            onPaymentPinChange = {
                submit(AddBankCardAction.PaymentPinChanged(it), true)
            },
            onPinVisibilityChange = {
                submit(AddBankCardAction.PinVisibilityChanged(it))
            },
            onCvvChange = { submit(AddBankCardAction.CvvChanged(it), true) },
            onCvvVisibilityChange = {
                submit(AddBankCardAction.CvvVisibilityChanged(it))
            },
            onExpiryMonthChange = {
                submit(AddBankCardAction.ExpiryMonthChanged(it), true)
            },
            onExpiryYearChange = {
                submit(AddBankCardAction.ExpiryYearChanged(it), true)
            },
            onTagsChange = { submit(AddBankCardAction.TagsChanged(it), true) },
            onBillingAddressChange = {
                submit(AddBankCardAction.BillingAddressChanged(it), true)
            },
            onNotesChange = { submit(AddBankCardAction.NotesChanged(it), true) },
        ),
        snackbarHostState = snackbarHostState,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
    )
}

private fun CardType.toUiType() = BankCardTypeUi.valueOf(name)

private fun BankCardTypeUi.toFeatureType() = CardType.valueOf(name)
