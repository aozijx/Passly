package com.aozijx.passly.core.otp

data class TotpState(
    val code: String = "------",
    val progress: Float = 1f,
    val decryptedSecret: String? = null
)