package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailBankCardUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailFieldUiModel
import com.aozijx.passly.presentation.feature.vault.detail.ui.model.DetailSshUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class DetailInteractionCallbacksTest {
    @Test
    fun `bank card reveal all selects only present hidden fields`() {
        val card = DetailBankCardUiModel(
            cardholder = null,
            cardholderRevealed = false,
            cardNumber = null,
            cardNumberRevealed = false,
            hasCardNumber = true,
            cvv = null,
            cvvRevealed = true,
            hasCvv = true,
            expiration = null,
            paymentPin = null,
            paymentPinRevealed = false,
            hasPaymentPin = false,
            editingCardholder = false,
            editedCardholder = "",
            editingCardNumber = false,
            editedCardNumber = "",
            editingCvv = false,
            editedCvv = "",
            canRevealMore = true,
        )

        assertEquals(
            setOf(DetailFieldUiModel.CARD_NUMBER, DetailFieldUiModel.CARDHOLDER),
            card.fieldsToReveal(),
        )
    }

    @Test
    fun `ssh reveal all excludes absent and already revealed fields`() {
        val ssh = DetailSshUiModel(
            fingerprint = "fingerprint",
            hasPassphrase = true,
            hasPrivateKey = true,
            passphrase = null,
            passphraseRevealed = false,
            privateKey = "revealed",
            privateKeyRevealed = true,
            isEditingPassphrase = false,
            editedPassphrase = "",
            canRevealMore = true,
        )

        assertEquals(setOf(DetailFieldUiModel.SSH_PASSPHRASE), ssh.fieldsToReveal())
    }
}
