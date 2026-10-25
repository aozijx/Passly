package com.aozijx.passly.domain.entry.model.credential

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CardExpiryTest {
    @Test
    fun `month and four digit year round trip including historical values`() {
        listOf(CardExpiry(3, 2030), CardExpiry(12, 1998), CardExpiry(1, 1), CardExpiry(12, 9999))
            .forEach { expiry ->
                assertEquals(CardExpiryParseResult.Parsed(expiry), CardExpiry.parse(expiry.storageValue))
            }
        assertEquals("03/2030", CardExpiry(3, 2030).storageValue)
        assertEquals("01/0001", CardExpiry(1, 1).storageValue)
    }

    @Test
    fun `parser rejects invalid months short years and noncanonical representations`() {
        listOf("00/2030", "13/2030", "03/0000", "03/30", "3/2030", "03/10000", " 03/2030", "０３/２０３０")
            .forEach { value -> assertEquals(CardExpiryParseResult.Invalid, CardExpiry.parse(value)) }
    }

    @Test
    fun `construction cannot create invalid month or year states`() {
        listOf(0 to 2030, 13 to 2030, 3 to 0, 3 to 10000).forEach { (month, year) ->
            assertThrows(IllegalArgumentException::class.java) { CardExpiry(month, year) }
        }
    }
}
