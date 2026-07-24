package com.aozijx.passly.domain.model.entry

import com.aozijx.passly.domain.model.entry.secret.LoginSecret
import com.aozijx.passly.domain.model.entry.secret.OtpSecret
import com.aozijx.passly.domain.model.entry.secret.WifiSecret
import com.aozijx.passly.domain.model.otp.OtpConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryCapabilityFlagsTest {

    @Test
    fun passwordAndOtp_canCoexist() {
        val flags = EntryCapabilityFlags.computeFrom(
            EntrySecret(
                login = LoginSecret(password = "password"),
                otp = OtpSecret(OtpConfig(secret = "JBSWY3DPEHPK3PXP"))
            )
        )

        assertTrue(EntryCapabilityFlags.has(flags, EntryCapabilityFlags.HAS_PASSWORD))
        assertTrue(EntryCapabilityFlags.has(flags, EntryCapabilityFlags.HAS_OTP))
    }

    @Test
    fun attachments_areExplicitAndIndependentFromSecret() {
        val withoutAttachment = EntryCapabilityFlags.computeFrom(EntrySecret())
        val withAttachment = EntryCapabilityFlags.computeFrom(
            secret = EntrySecret(),
            hasAttachments = true
        )

        assertFalse(
            EntryCapabilityFlags.has(
                withoutAttachment,
                EntryCapabilityFlags.HAS_ATTACHMENTS
            )
        )
        assertTrue(
            EntryCapabilityFlags.has(
                withAttachment,
                EntryCapabilityFlags.HAS_ATTACHMENTS
            )
        )
    }

    @Test
    fun wifiPassword_setsPasswordCapability() {
        val flags = EntryCapabilityFlags.computeFrom(
            EntrySecret(wifi = WifiSecret(password = "wifi-password"))
        )

        assertTrue(EntryCapabilityFlags.has(flags, EntryCapabilityFlags.HAS_PASSWORD))
        assertTrue(EntryCapabilityFlags.has(flags, EntryCapabilityFlags.HAS_WIFI))
    }
}
