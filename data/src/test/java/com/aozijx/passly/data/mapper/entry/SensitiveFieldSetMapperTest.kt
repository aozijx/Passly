package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.domain.entry.model.EntryHighSensitivitySecret
import com.aozijx.passly.domain.entry.model.HighSensitivityCardSecret
import com.aozijx.passly.domain.entry.model.HighSensitivityIdentitySecret
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SensitiveFieldSetMapperTest {
    @Test
    fun `high sensitivity aggregate splits into independent field values`() {
        val fields = EntryHighSensitivitySecret(
            card = HighSensitivityCardSecret(cardNumber = "4111", cardCvv = "123"),
            identity = HighSensitivityIdentitySecret(recoveryCodes = listOf("one", "two"))
        ).toSensitiveFieldValues()

        assertEquals("4111", fields[SensitiveFieldKey.CARD_NUMBER])
        assertEquals("123", fields[SensitiveFieldKey.CARD_CVV])
        assertEquals("one\ntwo", fields[SensitiveFieldKey.RECOVERY_CODES])
        assertNull(fields[SensitiveFieldKey.CARD_PAYMENT_PIN])
    }

    @Test
    fun `independent field values rebuild trusted export aggregate`() {
        val aggregate = mapOf(
            SensitiveFieldKey.CARD_CVV to "123",
            SensitiveFieldKey.RECOVERY_CODES to "one\ntwo"
        ).toHighSensitivitySecret()

        assertEquals("123", aggregate.card?.cardCvv)
        assertEquals(listOf("one", "two"), aggregate.identity?.recoveryCodes)
    }
}
