package com.aozijx.passly.domain.model

data class TotpState(
    val code: String = "------",
    val progress: Float = 1f,
    val decryptedSecret: String? = null
)