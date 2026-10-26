package com.aozijx.passly.feature.vault.presentation.editor.bankcard

sealed interface AddBankCardAction {
    data class TitleChanged(val value: String) : AddBankCardAction
    data class CardTypeChanged(val value: CardType) : AddBankCardAction
    data class CardholderChanged(val value: String) : AddBankCardAction
    data class CardNumberChanged(val value: String) : AddBankCardAction
    data class CardNumberVisibilityChanged(val visible: Boolean) : AddBankCardAction
    data class PaymentPinChanged(val value: String) : AddBankCardAction
    data class PinVisibilityChanged(val visible: Boolean) : AddBankCardAction
    data class CvvChanged(val value: String) : AddBankCardAction
    data class CvvVisibilityChanged(val visible: Boolean) : AddBankCardAction
    data class ExpiryChanged(val value: CardExpirySelection) : AddBankCardAction
    data class TagsChanged(val value: String) : AddBankCardAction
    data class BillingAddressChanged(val value: String) : AddBankCardAction
    data class IssuingBankChanged(val value: String) : AddBankCardAction
    data class BankBranchChanged(val value: String) : AddBankCardAction
    data class CardProductChanged(val value: String) : AddBankCardAction
    data class CurrencyChanged(val value: String) : AddBankCardAction
    data class BankPhoneChanged(val value: String) : AddBankCardAction
    data class NotesChanged(val value: String) : AddBankCardAction
    data object Save : AddBankCardAction
}
