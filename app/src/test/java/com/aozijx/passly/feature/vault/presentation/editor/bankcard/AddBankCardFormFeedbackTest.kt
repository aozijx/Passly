package com.aozijx.passly.feature.vault.presentation.editor.bankcard

import com.aozijx.passly.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddBankCardFormFeedbackTest {
    @Test
    fun `card number feedback maps domain validation to display messages`() {
        val cases =
            listOf(
                "123" to R.string.card_number_invalid_length,
                "4111x111111111111" to R.string.card_number_invalid_characters,
                "4111111111111112" to R.string.card_number_checksum_failed,
            )

        cases.forEach { (number, messageRes) ->
            val feedback = AddBankCardFormState(cardNumber = number).cardNumberFeedback

            assertEquals(BankCardFieldFeedbackUiState.Message(messageRes), feedback)
            assertTrue(feedback.isError)
        }
    }

    @Test
    fun `card number feedback is hidden until there is a validation error`() {
        listOf("", "   ", "4111111111111111").forEach { number ->
            val feedback = AddBankCardFormState(cardNumber = number).cardNumberFeedback

            assertEquals(BankCardFieldFeedbackUiState.Hidden, feedback)
            assertFalse(feedback.isError)
        }
    }

    @Test
    fun `cvv feedback keeps blank optional and valid values clear`() {
        listOf("", "   ", "123", "1234").forEach { cvv ->
            val feedback = AddBankCardFormState(cardCvv = cvv).cvvFeedback

            assertEquals(BankCardFieldFeedbackUiState.Hidden, feedback)
            assertFalse(feedback.isError)
        }
    }

    @Test
    fun `invalid cvv feedback exposes its display message`() {
        val feedback = AddBankCardFormState(cardCvv = "12").cvvFeedback

        assertEquals(
            BankCardFieldFeedbackUiState.Message(R.string.vault_card_cvv_invalid),
            feedback,
        )
        assertTrue(feedback.isError)
    }
}
