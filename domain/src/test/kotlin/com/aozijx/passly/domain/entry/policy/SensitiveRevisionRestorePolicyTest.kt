package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitiveRevisionRestorePolicyTest {
    @Test
    fun `authorization must cover values added and values removed by restore`() {
        val historical = setOf(SensitiveFieldKey.CARD_CVV)
        val current = setOf(SensitiveFieldKey.CARD_PAYMENT_PIN)

        assertFalse(
            SensitiveRevisionRestorePolicy.isExactAuthorization(
                authorizedFields = historical,
                currentFields = current,
                historicalFields = historical,
            )
        )
        assertTrue(
            SensitiveRevisionRestorePolicy.isExactAuthorization(
                authorizedFields = current + historical,
                currentFields = current,
                historicalFields = historical,
            )
        )
    }

    @Test
    fun `empty sensitive restore cannot mint a sensitive permit`() {
        assertFalse(
            SensitiveRevisionRestorePolicy.isExactAuthorization(
                authorizedFields = emptySet(),
                currentFields = emptySet(),
                historicalFields = emptySet(),
            )
        )
    }
}
