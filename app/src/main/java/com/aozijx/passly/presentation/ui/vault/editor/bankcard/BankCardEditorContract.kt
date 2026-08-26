package com.aozijx.passly.presentation.ui.vault.editor.bankcard

import androidx.compose.runtime.Immutable

enum class BankCardTypeUi { DEBIT, CREDIT }

@Immutable
class BankCardEditorState(
    val title: String,
    val cardType: BankCardTypeUi?,
    val cardholder: String,
    val cardNumber: String,
    val cardNumberError: String?,
    val cardCvv: String,
    val paymentPin: String,
    val cardExpiryMonth: String,
    val cardExpiryYear: String,
    val billingAddress: String,
    val tags: String,
    val notes: String,
    val isCardNumberVisible: Boolean,
    val isCvvVisible: Boolean,
    val isPinVisible: Boolean,
    val isFormValid: Boolean,
    val canSave: Boolean,
    val isSaving: Boolean,
)

data class BankCardEditorEventHandler(
    val onBack: () -> Unit,
    val onSave: () -> Unit,
    val onTitleChange: (String) -> Unit,
    val onCardTypeChange: (BankCardTypeUi) -> Unit,
    val onCardholderChange: (String) -> Unit,
    val onCardNumberChange: (String) -> Unit,
    val onCardNumberVisibilityChange: (Boolean) -> Unit,
    val onPaymentPinChange: (String) -> Unit,
    val onPinVisibilityChange: (Boolean) -> Unit,
    val onCvvChange: (String) -> Unit,
    val onCvvVisibilityChange: (Boolean) -> Unit,
    val onExpiryMonthChange: (String) -> Unit,
    val onExpiryYearChange: (String) -> Unit,
    val onTagsChange: (String) -> Unit,
    val onBillingAddressChange: (String) -> Unit,
    val onNotesChange: (String) -> Unit,
)
