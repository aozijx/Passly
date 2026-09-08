package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetailSensitiveFieldKeyMapperTest {

    @Test
    fun `detail keys map to their domain sensitive fields`() {
        val expected = mapOf(
            RevealedFieldKey.PASSWORD to SensitiveFieldKey.PASSWORD,
            RevealedFieldKey.CARD_NUMBER to SensitiveFieldKey.CARD_NUMBER,
            RevealedFieldKey.CVV to SensitiveFieldKey.CARD_CVV,
            RevealedFieldKey.PAYMENT_PIN to SensitiveFieldKey.CARD_PAYMENT_PIN,
            RevealedFieldKey.SSH_PRIVATE_KEY to SensitiveFieldKey.SSH_PRIVATE_KEY,
            RevealedFieldKey.SSH_PASSPHRASE to SensitiveFieldKey.SSH_PASSPHRASE,
            RevealedFieldKey.SEED_PHRASE to SensitiveFieldKey.SEED_PHRASE,
            RevealedFieldKey.PASSKEY_DATA to SensitiveFieldKey.PASSKEY_PRIVATE_REFERENCE,
            RevealedFieldKey.ID_NUMBER to SensitiveFieldKey.IDENTITY_NUMBER,
            RevealedFieldKey.RECOVERY_CODES to SensitiveFieldKey.RECOVERY_CODES,
        )

        assertEquals(expected, expected.keys.associateWith(DetailSensitiveFieldKeyMapper::toDomain))
    }

    @Test
    fun `only detail reveal fields map back to UI keys`() {
        assertEquals(RevealedFieldKey.CVV, DetailSensitiveFieldKeyMapper.toUi(SensitiveFieldKey.CARD_CVV))
        assertNull(DetailSensitiveFieldKeyMapper.toUi(SensitiveFieldKey.OTP_SECRET))
        assertNull(DetailSensitiveFieldKeyMapper.toDomain("unknown"))
    }
}
