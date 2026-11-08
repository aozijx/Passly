package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.feature.vault.presentation.detail.model.DetailBankCardUiModel
import com.aozijx.passly.feature.vault.presentation.detail.model.ProtectedDetailFieldUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailSshUiModel
import com.aozijx.passly.feature.vault.presentation.detail.DetailField
import org.junit.Assert.assertEquals
import org.junit.Test

class DetailRevealSelectionTest {
    @Test
    fun `bank card reveal all selects only present hidden fields`() {
        val card =
            DetailBankCardUiModel(
                cardholder = null,
                cardNumber = ProtectedDetailFieldUiState.Hidden,
                cvv = ProtectedDetailFieldUiState.Revealed("123"),
                expiration = null,
                paymentPin = ProtectedDetailFieldUiState.Absent,
                editingCardholder = false,
                editedCardholder = "",
                editingCardNumber = false,
                editedCardNumber = "",
                editingCvv = false,
                editedCvv = "",
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
                passphrase = ProtectedDetailFieldUiState.Hidden,
                privateKey = ProtectedDetailFieldUiState.Revealed("revealed"),
                isEditingPassphrase = false,
                editedPassphrase = "",
            )

        assertEquals(setOf(DetailField.SSH_PASSPHRASE), ssh.fieldsToReveal())
    }
}
