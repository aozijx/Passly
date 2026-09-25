package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailBankCardUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailFieldUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailSshUiModel

internal fun DetailBankCardUiModel.fieldsToReveal(): Set<DetailFieldUiModel> = buildSet {
    if (hasCardNumber && !cardNumberRevealed) add(DetailFieldUiModel.CARD_NUMBER)
    if (hasCvv && !cvvRevealed) add(DetailFieldUiModel.CARD_CVV)
    if (hasPaymentPin && !paymentPinRevealed) add(DetailFieldUiModel.PAYMENT_PIN)
    if (!cardholderRevealed) add(DetailFieldUiModel.CARDHOLDER)
}

internal fun DetailSshUiModel.fieldsToReveal(): Set<DetailFieldUiModel> = buildSet {
    if (hasPrivateKey && !privateKeyRevealed) add(DetailFieldUiModel.SSH_PRIVATE_KEY)
    if (hasPassphrase && !passphraseRevealed) add(DetailFieldUiModel.SSH_PASSPHRASE)
}
