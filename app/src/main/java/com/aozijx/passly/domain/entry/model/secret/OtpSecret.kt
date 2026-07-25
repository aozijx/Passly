package com.aozijx.passly.domain.entry.model.secret

import com.aozijx.passly.domain.entry.model.otp.OtpConfig

data class OtpSecret(
    val config: OtpConfig? = null
)
