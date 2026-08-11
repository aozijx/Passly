package com.aozijx.passly.domain.entry.model.sensitive

import com.aozijx.passly.domain.entry.model.EntryId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitiveFieldPresenceTest {
    @Test
    fun `presence exposes only keys and not values`() {
        val presence = SensitiveFieldPresence(
            entryId = EntryId("card"),
            keys = setOf(SensitiveFieldKey.CARD_CVV)
        )

        assertTrue(SensitiveFieldKey.CARD_CVV in presence)
        assertFalse(SensitiveFieldKey.CARD_PAYMENT_PIN in presence)
    }
}
