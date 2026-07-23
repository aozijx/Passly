package com.aozijx.passly.domain.model.entry.secret

import com.aozijx.passly.domain.model.otp.OtpConfig

data class OtpSecret(
    val config: OtpConfig? = null
)
