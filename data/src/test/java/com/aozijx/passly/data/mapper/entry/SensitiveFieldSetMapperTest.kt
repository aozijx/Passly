package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.IdentityCredential
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SensitiveFieldSetMapperTest {
    @Test
    fun `card credential splits into independent field values`() {
        val fields = EntrySecret(
            credential = CardCredential(cardNumber = "4111", cardCvv = "123"),
        ).toSensitiveFieldValues()

        assertEquals("4111", fields[SensitiveFieldKey.CARD_NUMBER])
        assertEquals("123", fields[SensitiveFieldKey.CARD_CVV])
        assertNull(fields[SensitiveFieldKey.CARD_PAYMENT_PIN])
    }

    @Test
    fun `identity recovery codes use one stable storage value`() {
        val fields = EntrySecret(
            credential = IdentityCredential(recoveryCodes = listOf("one", "two")),
        ).toSensitiveFieldValues()

        assertEquals("one\ntwo", fields[SensitiveFieldKey.RECOVERY_CODES])
    }
}
