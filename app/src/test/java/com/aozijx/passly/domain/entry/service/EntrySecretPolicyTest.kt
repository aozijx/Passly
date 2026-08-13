package com.aozijx.passly.domain.entry.service

import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.secret.LoginSecret
import com.aozijx.passly.domain.entry.model.secret.OtpSecret
import org.junit.Assert.assertThrows
import org.junit.Test

class EntrySecretPolicyTest {

    @Test
    fun accountAcceptsOnlyEmptySecret() {
        EntrySecretPolicy.requireValid(EntryType.ACCOUNT, EntrySecret())

        assertThrows(IllegalArgumentException::class.java) {
            EntrySecretPolicy.requireValid(
                EntryType.ACCOUNT,
                EntrySecret(notes = "must live on a credential")
            )
        }
    }

    @Test
    fun loginAndOtpMustBeSeparateEntries() {
        assertThrows(IllegalArgumentException::class.java) {
            EntrySecretPolicy.requireValid(
                EntryType.LOGIN,
                EntrySecret(
                    login = LoginSecret(password = "secret"),
                    otp = OtpSecret(OtpConfig(secret = "JBSWY3DPEHPK3PXP"))
                )
            )
        }
    }

    @Test
    fun otpAcceptsOtpPayload() {
        EntrySecretPolicy.requireValid(
            EntryType.OTP,
            EntrySecret(otp = OtpSecret(OtpConfig(secret = "JBSWY3DPEHPK3PXP")))
        )
    }
}
