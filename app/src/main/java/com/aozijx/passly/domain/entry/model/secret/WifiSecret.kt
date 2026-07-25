package com.aozijx.passly.domain.entry.model.secret

data class WifiSecret(
    val password: String? = null,
    val securityType: String? = null,
    val isHidden: Boolean = false
)
