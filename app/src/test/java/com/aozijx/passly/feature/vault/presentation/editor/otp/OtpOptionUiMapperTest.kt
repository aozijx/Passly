package com.aozijx.passly.feature.vault.presentation.editor.otp

import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.feature.vault.otp.presentation.plugin
import org.junit.Assert.assertEquals
import org.junit.Test

class OtpOptionUiMapperTest {
    @Test
    fun otpTypesMapToLocalizedLabels() {
        assertEquals(R.string.otp_type_totp, OtpType.TOTP.plugin().labelResource)
        assertEquals(R.string.otp_type_hotp, OtpType.HOTP.plugin().labelResource)
        assertEquals(R.string.otp_type_steam, OtpType.STEAM.plugin().labelResource)
        assertEquals("123 456", OtpType.TOTP.plugin().formatCode("123456"))
        assertEquals("123 456", OtpType.HOTP.plugin().formatCode("123456"))
        assertEquals("123456", OtpType.STEAM.plugin().formatCode("123456"))
    }

    @Test
    fun hashAlgorithmsMapToLocalizedLabels() {
        assertEquals(R.string.otp_algorithm_sha1, OtpHashAlgorithm.SHA1.labelResource)
        assertEquals(R.string.otp_algorithm_sha256, OtpHashAlgorithm.SHA256.labelResource)
        assertEquals(R.string.otp_algorithm_sha512, OtpHashAlgorithm.SHA512.labelResource)
    }

    @Test
    fun secretEncodingsMapToLocalizedLabels() {
        assertEquals(R.string.otp_encoding_base32, OtpSecretEncoding.BASE32.labelResource)
        assertEquals(R.string.otp_encoding_base64, OtpSecretEncoding.BASE64.labelResource)
    }
}
