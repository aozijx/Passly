package com.aozijx.passly.data.model.serializer

import com.aozijx.passly.domain.model.entry.VaultCredential
import com.aozijx.passly.domain.model.otp.OtpConfig
import com.aozijx.passly.domain.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.model.otp.OtpType
import org.junit.Assert.assertEquals
import org.junit.Test

class OtpConfigSerializationTest {

    @Test
    fun `credential serialization preserves complete Steam config`() {
        val credential = VaultCredential(
            entryId = "steam-entry",
            otp = OtpConfig(
                type = OtpType.STEAM,
                secret = "mixedCaseBase64Secret==",
                algorithm = OtpHashAlgorithm.SHA1,
                digits = 5,
                periodSeconds = 30,
                encoding = OtpSecretEncoding.BASE64,
                issuer = "Steam",
                accountName = "player"
            )
        )

        val json = AppJson.encodeToString(VaultCredential.serializer(), credential)
        val decoded = AppJson.decodeFromString(VaultCredential.serializer(), json)

        assertEquals(credential.otp, decoded.otp)
        assertEquals(OtpType.STEAM, decoded.otp?.type)
        assertEquals(OtpSecretEncoding.BASE64, decoded.otp?.encoding)
        assertEquals("mixedCaseBase64Secret==", decoded.otp?.secret)
    }
}
