package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.vault.presentation.detail.DetailField
import org.junit.Assert.assertEquals
import org.junit.Test

class DetailRevealSelectionTest {
    @Test
    fun `bank card reveal all selects only present hidden fields`() {
        val state =
            DetailUiState(
                sensitiveFieldKeys =
                    setOf(
                        SensitiveFieldKey.CARD_NUMBER,
                        SensitiveFieldKey.CARD_CVV,
                        SensitiveFieldKey.CARD_PAYMENT_PIN,
                    ),
                revealedFields = mapOf(FieldKey.CARD_CVV to TestSensitiveValue("123")),
            )

        assertEquals(
            setOf(DetailField.CARD_NUMBER, DetailField.PAYMENT_PIN),
            state.bankCardFieldsToReveal(),
        )
    }

    @Test
    fun `ssh reveal all excludes absent and already revealed fields`() {
        val state =
            DetailUiState(
                sensitiveFieldKeys = setOf(SensitiveFieldKey.SSH_PRIVATE_KEY, SensitiveFieldKey.SSH_PASSPHRASE),
                revealedFields = mapOf(FieldKey.SSH_KEY to TestSensitiveValue("revealed")),
            )

        assertEquals(setOf(DetailField.SSH_PASSPHRASE), state.sshFieldsToReveal())
    }

    private class TestSensitiveValue(private val value: String) : SensitiveValue {
        override val isEmpty: Boolean get() = value.isEmpty()

        override fun toCharArray(): CharArray = value.toCharArray()

        override fun wipe() = Unit
    }
}
