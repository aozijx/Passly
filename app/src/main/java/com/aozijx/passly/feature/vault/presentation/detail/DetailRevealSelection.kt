package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailBankCardUiModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.DetailSshUiModel

internal fun DetailBankCardUiModel.fieldsToReveal(): Set<DetailField> =
    buildSet {
        if (hasCardNumber && !cardNumberRevealed) add(DetailField.CARD_NUMBER)
        if (hasCvv && !cvvRevealed) add(DetailField.CARD_CVV)
        if (hasPaymentPin && !paymentPinRevealed) add(DetailField.PAYMENT_PIN)
    }

internal fun DetailSshUiModel.fieldsToReveal(): Set<DetailField> =
    buildSet {
        if (hasPrivateKey && !privateKeyRevealed) add(DetailField.SSH_PRIVATE_KEY)
        if (hasPassphrase && !passphraseRevealed) add(DetailField.SSH_PASSPHRASE)
    }
