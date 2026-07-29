package com.aozijx.passly.feature.vault.editor.otp

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.feature.vault.model.OtpFormState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OtpEntryFactoryTest {

    @Test
    fun createMapsTotpFieldsAndNormalizesMetadata() {
        val entry = OtpEntryFactory.create(
            state = OtpFormState(
                title = "  Example  ",
                username = "  user@example.com  ",
                domain = "  example.com  ",
                issuer = "  Example Inc  ",
                secret = "  JBSWY3DPEHPK3PXP  ",
                period = "60",
                digits = "8",
                algorithm = "SHA256",
                encoding = OtpSecretEncoding.BASE32
            ),
            now = 123L
        )

        val config = requireNotNull(entry.secret.otp?.config)
        assertEquals(EntryType.OTP, entry.entryType)
        assertEquals("Example", entry.summary.title)
        assertEquals("user@example.com", entry.summary.username)
        assertEquals("example.com", entry.summary.website?.primaryUrl)
        assertEquals(OtpType.TOTP, config.type)
        assertEquals("JBSWY3DPEHPK3PXP", config.secret)
        assertEquals(60, config.periodSeconds)
        assertEquals(8, config.digits)
        assertEquals(OtpHashAlgorithm.SHA256, config.algorithm)
        assertEquals("Example Inc", config.issuer)
        assertEquals("user@example.com", config.accountName)
        assertEquals(123L, entry.header.createdAt)
        assertEquals(123L, entry.header.updatedAt)
    }

    @Test
    fun createUsesTypeSpecificHotpAndSteamValues() {
        val hotp = requireNotNull(
            OtpEntryFactory.create(
                OtpFormState(
                    title = "HOTP",
                    secret = "secret",
                    type = OtpType.HOTP,
                    counter = "42"
                )
            ).secret.otp?.config
        )
        val steam = requireNotNull(
            OtpEntryFactory.create(
                OtpFormState(
                    title = "Steam",
                    secret = "secret",
                    type = OtpType.STEAM,
                    digits = "8"
                )
            ).secret.otp?.config
        )

        assertNull(hotp.periodSeconds)
        assertEquals(42L, hotp.counter)
        assertEquals(5, steam.digits)
        assertNull(steam.counter)
    }
}
