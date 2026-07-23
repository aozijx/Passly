package com.aozijx.passly.domain.model.entry.secret

data class WifiSecret(
    val password: String? = null,
    val securityType: String? = null,
    val isHidden: Boolean = false
)
