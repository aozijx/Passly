package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.feature.vault.presentation.detail.model.DetailBankCardUiModel
import com.aozijx.passly.feature.vault.presentation.detail.DetailField
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailSshUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class DetailRevealSelectionTest {
    @Test
    fun `bank card reveal all selects only present hidden fields`() {
        val card =
            DetailBankCardUiModel(
                cardholder = null,
                cardholderRevealed = true,
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
            setOf(DetailField.CARD_NUMBER),
            card.fieldsToReveal(),
        )
    }

    @Test
    fun `ssh reveal all excludes absent and already revealed fields`() {
        val ssh =
            DetailSshUiModel(
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

        assertEquals(setOf(DetailField.SSH_PASSPHRASE), ssh.fieldsToReveal())
    }
}
