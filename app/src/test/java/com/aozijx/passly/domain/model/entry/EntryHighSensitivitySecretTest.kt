package com.aozijx.passly.domain.model.entry

import com.aozijx.passly.domain.entry.model.EntryHighSensitivitySecret
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.HighSensitivityCardSecret
import com.aozijx.passly.domain.entry.model.mergeWith
import com.aozijx.passly.domain.entry.model.secret.CardSecret
import com.aozijx.passly.domain.entry.model.withoutHighSensitivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryHighSensitivitySecretTest {

    @Test
    fun `withoutHighSensitivity keeps card presence hints and strips real values`() {
        val secret = EntrySecret(
            card = CardSecret(
                cardNumber = "4111111111111111",
                cardCvv = "123",
                paymentPin = "000000"
            )
        )

        val stripped = secret.withoutHighSensitivity().card

        assertNull(stripped?.cardNumber)
        assertNull(stripped?.cardCvv)
        assertNull(stripped?.paymentPin)
        assertTrue(stripped?.hasCardNumber == true)
        assertTrue(stripped?.hasCardCvv == true)
        assertTrue(stripped?.hasPaymentPin == true)
    }

    @Test
    fun `mergeWith updates card fields without dropping existing high sensitivity fields`() {
        val current = EntryHighSensitivitySecret(
            card = HighSensitivityCardSecret(
                cardNumber = "4111111111111111",
                cardCvv = "123",
                paymentPin = "000000"
            )
        )
        val update = EntryHighSensitivitySecret(
            card = HighSensitivityCardSecret(cardCvv = "999")
        )

        val merged = current.mergeWith(update).card

        assertEquals("4111111111111111", merged?.cardNumber)
        assertEquals("999", merged?.cardCvv)
        assertEquals("000000", merged?.paymentPin)
    }
}
