package com.aozijx.passly.feature.vault.presentation.editor.otp

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.feature.vault.otp.presentation.plugin

internal val OtpType.labelResource: Int
    @StringRes get() = plugin().labelResource

internal val OtpHashAlgorithm.labelResource: Int
    @StringRes get() =
        when (this) {
            OtpHashAlgorithm.SHA1 -> R.string.otp_algorithm_sha1
            OtpHashAlgorithm.SHA256 -> R.string.otp_algorithm_sha256
            OtpHashAlgorithm.SHA512 -> R.string.otp_algorithm_sha512
        }

internal val OtpSecretEncoding.labelResource: Int
    @StringRes get() =
        when (this) {
            OtpSecretEncoding.BASE32 -> R.string.otp_encoding_base32
            OtpSecretEncoding.BASE64 -> R.string.otp_encoding_base64
        }
