package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.domain.entry.model.FieldKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetailSensitiveFieldKeyMapperTest {

    @Test
    fun `detail reveal keys map to semantic entry fields`() {
        val expected = mapOf(
            RevealedFieldKey.USERNAME to FieldKey.USERNAME,
            RevealedFieldKey.PASSWORD to FieldKey.PASSWORD,
            RevealedFieldKey.CARDHOLDER to FieldKey.CARD_HOLDER,
            RevealedFieldKey.CARD_NUMBER to FieldKey.CARD_NUMBER,
            RevealedFieldKey.CVV to FieldKey.CARD_CVV,
            RevealedFieldKey.PAYMENT_PIN to FieldKey.PAYMENT_PIN,
            RevealedFieldKey.SSH_PRIVATE_KEY to FieldKey.SSH_KEY,
            RevealedFieldKey.SSH_PASSPHRASE to FieldKey.SSH_PASSPHRASE,
            RevealedFieldKey.SEED_PHRASE to FieldKey.SEED_PHRASE,
            RevealedFieldKey.PASSKEY_DATA to FieldKey.PASSKEY_DATA,
            RevealedFieldKey.ID_NUMBER to FieldKey.ID_NUMBER,
            RevealedFieldKey.RECOVERY_CODES to FieldKey.RECOVERY_CODES,
        )

        assertEquals(expected, expected.keys.associateWith(DetailSensitiveFieldKeyMapper::toFieldKey))
    }

    @Test
    fun `entry fields map back only when detail can render them`() {
        assertEquals(RevealedFieldKey.CVV, DetailSensitiveFieldKeyMapper.toUiKey(FieldKey.CARD_CVV))
        assertNull(DetailSensitiveFieldKeyMapper.toUiKey(FieldKey.OTP_SECRET))
        assertNull(DetailSensitiveFieldKeyMapper.toFieldKey("unknown"))
    }
}
