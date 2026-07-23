package com.aozijx.passly.domain.model.entry.secret

data class SshSecret(
    val privateKey: String? = null,
    val publicKey: String? = null,
    val passphrase: String? = null
)
