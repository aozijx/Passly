package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.feature.vault.presentation.detail.model.DetailBankCardUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailSshUiModel

internal fun DetailBankCardUiModel.fieldsToReveal(): Set<DetailField> =
    buildSet {
        if (cardNumber.isPresent && !cardNumber.isRevealed) add(DetailField.CARD_NUMBER)
        if (hasCvv && !cvv.isRevealed) add(DetailField.CARD_CVV)
        if (paymentPin.isPresent && !paymentPin.isRevealed) add(DetailField.PAYMENT_PIN)
    }

internal fun DetailSshUiModel.fieldsToReveal(): Set<DetailField> =
    buildSet {
        if (privateKey.isPresent && !privateKey.isRevealed) add(DetailField.SSH_PRIVATE_KEY)
        if (passphrase.isPresent && !passphrase.isRevealed) add(DetailField.SSH_PASSPHRASE)
    }
