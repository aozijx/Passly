package com.aozijx.passly.domain.entry.model.secret

data class SshSecret(
    val privateKey: String? = null,
    val publicKey: String? = null,
    val passphrase: String? = null
)
