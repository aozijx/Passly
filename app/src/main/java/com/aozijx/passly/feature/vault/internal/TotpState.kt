package com.aozijx.passly.feature.vault.internal

data class TotpState(
    val code: String = "------",
    val progress: Float = 1f,
    val decryptedSecret: String? = null
)
